val kyoriTextVersion = "3.0.2"

dependencies {
    compile("net.kyori:text-api:${kyoriTextVersion}")
    compile("net.kyori:text-serializer-gson:${kyoriTextVersion}")
    compile("net.kyori:text-serializer-legacy:${kyoriTextVersion}")

    // Updated dependencies
    compile("com.google.code.gson:gson:2.8.5")
}