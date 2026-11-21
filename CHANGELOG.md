## [1.0.0-dev.12](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.11...v1.0.0-dev.12) (2026-11-21)

### 🐛 Bug Fixes

* **samsungmessages:** prevent flip cover crash and enable group MMS by default ([cb30bbd](https://github.com/boredclawd/morphe-patches/commit/cb30bbd52ea4e7d224f68217a0d9b9660b740f91))

## [1.0.0-dev.11](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.10...v1.0.0-dev.11) (2026-11-21)

### 🐛 Bug Fixes

* **samsungmessages:** fix conversation sync and settings crash ([9daba62](https://github.com/boredclawd/morphe-patches/commit/9daba627332a4c7c402a40bec9a965c9ebb96ee2))

## [1.0.0-dev.10](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.9...v1.0.0-dev.10) (2026-11-21)

### 🐛 Bug Fixes

* **samsungmessages:** fix smali field syntax for Boolean.FALSE in IMS tasks ([2acdb1b](https://github.com/boredclawd/morphe-patches/commit/2acdb1bbe0b003bf6e5e90514f616e572726e151))

## [1.0.0-dev.9](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.8...v1.0.0-dev.9) (2026-11-21)

### 🐛 Bug Fixes

* return empty cursor on query failure and stub killMessage, CB and IMS tasks ([7a56c77](https://github.com/boredclawd/morphe-patches/commit/7a56c77c9247c893410486fc3bfdf6aa74363057))
* use literal query projection arg in emptyCursor ([2008be8](https://github.com/boredclawd/morphe-patches/commit/2008be809e83a3f26c8a0944a64768b56a338d1b))

## [1.0.0-dev.8](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.7...v1.0.0-dev.8) (2026-11-21)

### 🐛 Bug Fixes

* wrap all ContentResolver operations to prevent provider SecurityException ([4262e87](https://github.com/boredclawd/morphe-patches/commit/4262e87c6e8e7b37a999d750768ab2cae414e84d))

## [1.0.0-dev.7](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.6...v1.0.0-dev.7) (2026-11-21)

### 🐛 Bug Fixes

* match exact method return types in SemImsManagerImpl and ImsManagerImpl stubs ([f248d56](https://github.com/boredclawd/morphe-patches/commit/f248d565374de366d231e30e6d67adf43e372ee9))

## [1.0.0-dev.6](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.5...v1.0.0-dev.6) (2026-11-21)

### 🐛 Bug Fixes

* wrap Telephony calls and neutralize IMS and SimMobility permissions ([6edc914](https://github.com/boredclawd/morphe-patches/commit/6edc914f1c070ef18d26bf35c81f4e804fed29a7))

## [1.0.0-dev.5](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.4...v1.0.0-dev.5) (2026-11-21)

### 🐛 Bug Fixes

* **extension:** invoke hidden registerContentObserver overload via reflection ([1735beb](https://github.com/boredclawd/morphe-patches/commit/1735beb0599343b1d53debcc44e776a630df6745))
* intercept restricted CMC content observers and stub CmcFeature ([d416b5e](https://github.com/boredclawd/morphe-patches/commit/d416b5ea80c8ba6d20cd8d51d8a76e16b25fff78))

## [1.0.0-dev.4](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2026-11-21)

### 🐛 Bug Fixes

* resolve invalid register warning and power-save startup crash in package rename patch ([2f4fd2d](https://github.com/boredclawd/morphe-patches/commit/2f4fd2d9cfeddd40a8aa56398c1956810d807afd))

## [1.0.0-dev.3](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.2...v1.0.0-dev.3) (2026-11-21)

### ✨ New Features

* add change package name patch for Samsung Messages ([914d66b](https://github.com/boredclawd/morphe-patches/commit/914d66b07be4c6c334f20fc5dc040f9debf45193))

## [1.0.0-dev.2](https://github.com/boredclawd/morphe-patches/compare/v1.0.0-dev.1...v1.0.0-dev.2) (2026-11-21)

### ✨ New Features

* add charged SMS and roaming confirmation alert patch for Samsung Messages ([77d5fed](https://github.com/boredclawd/morphe-patches/commit/77d5fed1e2924a6761c2a2896a227fe716ceb2e9))

## 1.0.0-dev.1 (2026-11-20)

### ✨ New Features

* add disable SMS to MMS conversion patch for Samsung Messages ([6f7d8fd](https://github.com/boredclawd/morphe-patches/commit/6f7d8fdd0b02eddb16cabc0a938ca10c60b5d431))
