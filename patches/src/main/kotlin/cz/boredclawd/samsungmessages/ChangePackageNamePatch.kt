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
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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

private val VIRTUAL = setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE)
private val STATIC = setOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE)

private fun MethodReference.shape() = "$name(${parameterTypes.joinToString("")})$returnType"

internal fun BytecodePatchContext.redirectCalls(target: String, calls: Map<String, Set<String>>): Int {
    fun Any.match(): MethodReference? {
        if (this !is ReferenceInstruction || (opcode !in VIRTUAL && opcode !in STATIC)) return null
        val r = reference as? MethodReference ?: return null
        return if (calls[r.definingClass]?.contains(r.shape()) == true) r else null
    }

    val sites = mutableListOf<Pair<String, Method>>()
    classDefForEach { c ->
        if (c.type.startsWith("Lcz/boredclawd/")) return@classDefForEach
        for (m in c.methods) {
            if (m.implementation?.instructions?.any { it.match() != null } == true) sites += c.type to m
        }
    }
    var moved = 0
    for ((type, method) in sites) {
        val mutable = mutableClassDefBy(type).methods.first {
            it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType
        }
        val instructions = mutable.implementation!!.instructions
        for (index in instructions.indices) {
            val insn = instructions[index]
            val r = insn.match() ?: continue
            val receiver = if (insn.opcode in VIRTUAL) r.definingClass else ""
            val to = "$target->${r.name}($receiver${r.parameterTypes.joinToString("")})${r.returnType}"
            val call = when (insn) {
                is FiveRegisterInstruction -> {
                    val regs = listOf(insn.registerC, insn.registerD, insn.registerE, insn.registerF, insn.registerG)
                        .take(insn.registerCount).map { registerName(it, method) }
                    regs.joinToString(", ", "invoke-static { ", " }, $to")
                }
                is RegisterRangeInstruction -> {
                    val start = insn.startRegister
                    val count = insn.registerCount
                    val startName = registerName(start, method)
                    val endName = registerName(start + count - 1, method)
                    "invoke-static/range { $startName .. $endName }, $to"
                }
                else -> throw PatchException("unexpected ${insn.opcode} in $type.${method.name}")
            }
            mutable.replaceInstruction(index, call)
            moved++
        }
    }
    return moved
}

private fun BytecodePatchContext.redirectToWrappers(target: String, owners: Set<String>): Int {
    val calls = mutableMapOf<String, MutableSet<String>>()
    for (m in mutableClassDefBy(target).methods) {
        if (!com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(m.accessFlags) || !com.android.tools.smali.dexlib2.AccessFlags.PUBLIC.isSet(m.accessFlags)) continue
        val parameters = m.parameterTypes.map { it.toString() }
        val owner = parameters.firstOrNull() ?: continue
        if (owner !in owners) continue
        calls.getOrPut(owner) { mutableSetOf() } += "${m.name}(${parameters.drop(1).joinToString("")})${m.returnType}"
    }
    return redirectCalls(target, calls)
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
    extendWith(SAMSUNG_MESSAGES_EXTENSION)

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

                    for (tag in listOf("uses-library", "uses-native-library")) {
                        val nodes = manifest.getElementsByTagName(tag)
                        for (i in 0 until nodes.length) {
                            (nodes.item(i) as Element).setAttribute("android:required", "false")
                        }
                    }

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

        // Neutralize Call & Message Continuity (CMC) features that require privileged CMC_ES_PERMISSION
        val cmcFeature = classDefByOrNull("Lcom/samsung/android/messaging/common/configuration/cmc/CmcFeature;")
        if (cmcFeature != null) {
            val mutableCmc = mutableClassDefBy("Lcom/samsung/android/messaging/common/configuration/cmc/CmcFeature;")
            mutableCmc.methods.filter { it.name == "registerInitialListeners" }.forEach {
                it.addInstructions(0, "return-void")
            }
            mutableCmc.methods.filter {
                it.name in setOf("isCmcOpenSecondaryDevice", "getEnableCmcOpenService", "isCmcOpenPrimaryDevice", "isCmcOpenActive", "isCmcWatchActive")
            }.forEach {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
            mutableCmc.methods.filter { it.name == "getCmcSettingManager" && it.parameterTypes.isEmpty() }.forEach {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
            }
            log.info("Patched CmcFeature to disable inaccessible CMC listeners")
        }

        // Catch SecurityException on any restricted ContentObserver registrations
        val observers = mapOf(
            "Landroid/content/ContentResolver;" to setOf(
                "registerContentObserver(Landroid/net/Uri;ZLandroid/database/ContentObserver;)V",
                "registerContentObserver(Landroid/net/Uri;ZLandroid/database/ContentObserver;I)V",
            ),
        )
        val redirectedObservers = redirectCalls("Lcz/boredclawd/samsungmessages/Providers;", observers)
        log.info("Redirected $redirectedObservers ContentObserver registrations to safe wrapper")

        // Neutralize SimMobility listeners that require privileged IMS service
        val simMobility = classDefByOrNull("Lcom/samsung/android/messaging/common/util/SimMobility;")
        if (simMobility != null) {
            val mutableSimMobility = mutableClassDefBy("Lcom/samsung/android/messaging/common/util/SimMobility;")
            mutableSimMobility.methods.filter { it.name == "registerSimMobilityStatusListener" }.forEach {
                it.addInstructions(0, "return-void")
            }
            mutableSimMobility.methods.filter { it.name == "isSimMobility" }.forEach {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
            log.info("Patched SimMobility methods to prevent ImsService SecurityException")
        }

        // Neutralize IMS wrapper calls that access privileged Samsung IMS service
        listOf(
            "Lcom/samsung/android/messaging/seapiwrapper/factory/SemImsManagerImpl;",
            "Lcom/samsung/android/messaging/seapiwrapper/factory/ImsManagerImpl;",
        ).forEach { descriptor ->
            val cls = classDefByOrNull(descriptor) ?: return@forEach
            val mutableCls = mutableClassDefBy(descriptor)
            mutableCls.methods.filter { it.name != "<init>" && it.name != "<clinit>" }.forEach {
                when {
                    it.returnType == "V" -> it.addInstructions(0, "return-void")
                    it.returnType.startsWith("L") || it.returnType.startsWith("[") ->
                        it.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                    it.returnType in setOf("J", "D") ->
                        it.addInstructions(0, "const-wide/16 v0, 0x0\nreturn-wide v0")
                    else ->
                        it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                }
            }
            log.info("Neutralized all methods in $descriptor to disable inaccessible Samsung IMS features")
        }

        // Redirect telephony calls that fail without system permissions to safe wrappers
        val telephonyMoved = redirectToWrappers(
            "Lcz/boredclawd/samsungmessages/Telephony;",
            setOf("Landroid/telephony/TelephonyManager;", "Landroid/telephony/SubscriptionManager;", "Landroid/telephony/SmsManager;"),
        )
        log.info("Redirected $telephonyMoved Telephony calls to safe wrapper")
    }
}
