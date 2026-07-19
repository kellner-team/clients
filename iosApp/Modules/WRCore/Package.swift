// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "WRCore",
    platforms: [
        .iOS(.v15),
    ],
    products: [
        .library(
            name: "WRCore",
            targets: ["WRCore"]
        ),
    ],
    dependencies: [
        .package(path: "../SharedUI"),
    ],
    targets: [
        .target(
            name: "WRCore",
            dependencies: [
                .product(name: "SharedUI", package: "SharedUI"),
            ]
        ),
        .testTarget(
            name: "WRCoreTests",
            dependencies: ["WRCore"]
        ),
    ]
)
