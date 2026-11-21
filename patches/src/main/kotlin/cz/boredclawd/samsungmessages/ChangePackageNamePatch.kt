package cz.boredclawd.samsungmessages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import cz.boredclawd.samsungmessages.Constants.COMPATIBILITY_SAMSUNG_MESSAGES
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.logging.Logger

private const val STOCK_PACKAGE = "com.samsung.android.messaging"

private val log = Logger.getLogger("SamsungMessages")

private fun Document.elements(tag: String): List<Element> =
    getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }

private val renamed = mutableMapOf<String, String>()

private val CONST_STRING = setOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO)

private fun Any.literal(): String? =
    if (this is ReferenceInstruction && opcode in CONST_STRING) (reference as StringReference).string else null

private fun registerName(register: Int, method: Method): String {
    val impl = method.implementation ?: return "v$register"
    val totalRegisters = impl.registerCount
    val parameterCount = method.parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 } +
        (if (com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(method.accessFlags)) 0 else 1)
    val locals = totalRegisters - parameterCount
    return if (register < locals) "v$register" else "p${register - locals}"
}

/**
 * Rewrites every string literal in the app (not in our extensions) that [change] answers a new value for:
 * const-string instructions and the initial values of static String fields.
 */
internal fun BytecodePatchContext.rewriteStringLiterals(change: (String) -> String?): Int {
    val sites = mutableListOf<Pair<String, Method>>()
    val fieldSites = mutableListOf<Pair<String, String>>()
    classDefForEach { c ->
        if (c.type.startsWith("Lcz/boredclawd/")) return@classDefForEach
        for (f in c.staticFields) {
            val value = (f.initialValue as? StringEncodedValue)?.value ?: continue
            if (change(value) != null) fieldSites += c.type to f.name
        }
        for (m in c.methods) {
            val hit = m.implementation?.instructions?.any { insn -> insn.literal()?.let(change) != null } ?: false
            if (hit) sites += c.type to m
        }
    }
    var rewritten = 0
    for ((type, name) in fieldSites) {
        val field = mutableClassDefBy(type).fields.first { it.name == name }
        val value = field.initialValue as MutableStringEncodedValue
        value.setValue(change(value.value)!!)
        rewritten++
    }
    for ((type, method) in sites) {
        val mutable = mutableClassDefBy(type).methods.first {
            it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType
        }
        val instructions = mutable.implementation!!.instructions
        for (index in instructions.indices) {
            val insn = instructions[index]
            val to = insn.literal()?.let(change) ?: continue
            val escaped = to.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            val reg = registerName((insn as OneRegisterInstruction).registerA, method)
            mutable.replaceInstruction(index, "${if (insn.opcode == Opcode.CONST_STRING_JUMBO) "const-string/jumbo" else "const-string"} $reg, \"$escaped\"")
            rewritten++
        }
    }
    return rewritten
}

/**
 * Installs next to Samsung's stock Messages on Galaxy phones under its own package name so it can be installed
 * as an update or standalone user app on unrooted One UI and set as the default SMS app.
 *
 * Moves with the package: the package itself, provider authorities, and declared permissions (and uses-permission),
 * and rewrites string literals in bytecode matching those so the app talks to its own providers.
 */
@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change package name",
    description = "Installs next to Samsung's own Messages on Galaxy phones under its own package name.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SAMSUNG_MESSAGES)

    val packageName = stringOption(
        key = "packageName",
        default = "cz.boredclawd.messaging",
        title = "Package name",
        description = "Must be a valid Android package name.",
        required = true,
    ) {
        it != null && it != STOCK_PACKAGE && it.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))
    }

    dependsOn(
        resourcePatch(description = "Moves the manifest to the new package name.") {
            execute {
                val old = STOCK_PACKAGE
                val new = packageName.value!!
                fun under(value: String) = value == old || value.startsWith("$old.")
                fun moved(value: String) = if (under(value)) new + value.substring(old.length) else "$new.$value"
                renamed.clear()
                renamed[old] = new
                var authorities = 0
                var permissions = 0

                document("AndroidManifest.xml").use { manifest ->
                    val root = manifest.documentElement
                    if (root.getAttribute("package") != old) throw PatchException("not Samsung Messages manifest")
                    root.setAttribute("package", new)
                    root.removeAttribute("android:sharedUserId")
                    root.removeAttribute("coreApp")

                    val own = manifest.elements("permission").map { it.getAttribute("android:name") }.toSet()
                    for (element in manifest.elements("permission") + manifest.elements("uses-permission")) {
                        val name = element.getAttribute("android:name")
                        if (name !in own) continue
                        element.setAttribute("android:name", moved(name))
                        renamed[name] = moved(name)
                        if (element.tagName == "permission") permissions++
                    }
                    for (element in manifest.elements("*")) {
                        for (attribute in listOf("android:permission", "android:readPermission", "android:writePermission")) {
                            val value = element.getAttribute(attribute)
                            if (value in own) element.setAttribute(attribute, moved(value))
                        }
                        if (element.getAttribute("android:process") == old) element.setAttribute("android:process", new)
                    }

                    // Only the app's own providers, not <queries> entries naming other apps' ones.
                    val application = manifest.elements("application").single()
                    for (provider in manifest.elements("provider").filter { it.parentNode == application }) {
                        val list = provider.getAttribute("android:authorities").split(';')
                        val moved = list.map(::moved)
                        list.zip(moved).filter { (a, b) -> a != b }.forEach { (a, b) -> renamed[a] = b; authorities++ }
                        provider.setAttribute("android:authorities", moved.joinToString(";"))
                    }
                }

                log.info("Package rename: $authorities authorities, $permissions permissions")
                if (authorities < 20 || permissions < 15) {
                    throw PatchException("package rename found $authorities authorities and $permissions permissions")
                }
            }
        },
    )

    execute {
        val old = STOCK_PACKAGE
        val new = renamed.getValue(old)
        // Exact names, content:// URIs built on them, the app's process names ("<package>:...") and its
        // android.resource:// URIs.
        fun rename(s: String): String? {
            // Keep stock package string literals untouched (e.g. FloatingFeature defaults)
            if (s == old) return null
            renamed[s]?.let { return it }
            if (s.startsWith("$old:")) return new + s.substring(old.length)
            if (s.startsWith("android.resource://$old/")) return "android.resource://$new/" + s.substring("android.resource://$old/".length)
            if (s.startsWith("content://")) {
                val rest = s.removePrefix("content://")
                val hit = renamed.keys.filter { it != old && (rest == it || rest.startsWith("$it/")) }.maxByOrNull { it.length } ?: return null
                return "content://" + renamed.getValue(hit) + rest.substring(hit.length)
            }
            return null
        }

        val literals = rewriteStringLiterals(::rename)
        log.info("Package rename: $literals literals moved")
        if (literals < 10) throw PatchException("only $literals package, authority and permission literals found")

        // Neutralize power save whitelist registration to prevent startup SecurityException
        // when running as a non-system user app
        val deviceUtil = classDefByOrNull("Lcom/samsung/android/messaging/common/util/DeviceUtil;")
        if (deviceUtil != null) {
            val mutableDeviceUtil = mutableClassDefBy("Lcom/samsung/android/messaging/common/util/DeviceUtil;")
            mutableDeviceUtil.methods.find { it.name == "addToPowerSaveAllowlistApp" && it.parameterTypes.isEmpty() }
                ?.addInstructions(0, "return-void")
            mutableDeviceUtil.methods.find { it.name == "isPowerSaveAllowListed" && it.parameterTypes.isEmpty() }
                ?.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            log.info("Patched DeviceUtil power-save methods to prevent startup SecurityException")
        }
    }
}
