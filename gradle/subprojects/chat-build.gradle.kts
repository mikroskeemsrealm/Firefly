val kyoriTextVersion = "3.0.2"

dependencies {
    api("net.kyori:text-api:${kyoriTextVersion}")
    api("net.kyori:text-serializer-gson:${kyoriTextVersion}")
    api("net.kyori:text-serializer-legacy:${kyoriTextVersion}")

    // Updated dependencies
    api("com.google.code.gson:gson:2.8.5")
}