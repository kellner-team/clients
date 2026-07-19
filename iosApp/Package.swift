// swift-tools-version:5.9
import PackageDescription

// this package is only for command line dependencies, not used for dependencies used by any targets
let package = Package(
    name: "CMDLineDependencies",
    platforms: [
        .macOS(.v10_15),
    ],
    dependencies: [
        .package(url: "https://github.com/nicklockwood/SwiftFormat", from: "0.54.6"),
    ]
)
