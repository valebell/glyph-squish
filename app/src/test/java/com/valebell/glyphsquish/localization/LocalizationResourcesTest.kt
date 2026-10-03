package com.valebell.glyphsquish.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationResourcesTest {
    private val resourceRoot = File("src/main/res")
    private val localeFolders =
        mapOf(
            "zh-Hans" to "b+zh+Hans",
            "hi" to "hi",
            "es" to "es",
            "fr" to "fr",
            "ar" to "ar",
            "bn" to "bn",
            "pt" to "pt",
            "ru" to "ru",
            "ur" to "ur",
            "id" to "in",
        )

    @Test
    fun everyAdvertisedLanguageHasEveryTranslatableResourceAndMatchingArguments() {
        val defaults = resourcesIn("values").filterValues { it.getAttribute("translatable") != "false" }
        localeFolders.forEach { (locale, folder) ->
            val translated = resourcesIn("values-$folder")
            assertEquals("Translated keys for $locale", defaults.keys, translated.keys)
            defaults.forEach { (name, original) ->
                assertResource(locale, name, original, translated.getValue(name))
            }
        }
    }

    @Test
    fun systemLanguagePickerListsExactlyTheShippedLanguages() {
        val config = parse(File(resourceRoot, "xml/locales_config.xml"))
        val advertised =
            childElements(config)
                .map {
                    Locale.forLanguageTag(it.getAttribute("android:name")).toLanguageTag()
                }.toSet()
        assertEquals(localeFolders.keys + "en", advertised)
        val manifest = parse(File("src/main/AndroidManifest.xml"))
        val application = childElements(manifest).single { it.tagName == "application" }
        assertEquals("@xml/locales_config", application.getAttribute("android:localeConfig"))
        assertEquals("true", application.getAttribute("android:supportsRtl"))
    }

    private fun assertResource(
        locale: String,
        name: String,
        original: Element,
        localized: Element,
    ) {
        assertEquals("Resource type for $locale/$name", original.tagName, localized.tagName)
        val expectedArguments = arguments(texts(original).first())
        texts(localized).forEach { value ->
            assertFalse("Empty translation: $locale/$name", value.isBlank())
            assertEquals("Format arguments for $locale/$name", expectedArguments, arguments(value))
            assertEquals(
                "Balanced isolates in $locale/$name",
                value.count { it == '\u2066' },
                value.count { it == '\u2069' },
            )
        }
        if (localized.tagName == "plurals") {
            val quantities = childElements(localized).map { it.getAttribute("quantity") }.toSet()
            assertTrue("Fallback plural for $locale/$name", "other" in quantities)
            if (locale == "ar") {
                assertEquals(setOf("zero", "one", "two", "few", "many", "other"), quantities)
            }
        }
    }

    private fun resourcesIn(folder: String): Map<String, Element> {
        val entries =
            File(resourceRoot, folder)
                .listFiles()
                .orEmpty()
                .filter { it.extension == "xml" }
                .flatMap { childElements(parse(it)) }
                .filter { it.tagName in setOf("string", "plurals") }
        val byName = entries.associateBy { it.getAttribute("name") }
        assertEquals("Duplicate resource in $folder", entries.size, byName.size)
        return byName
    }

    private fun texts(element: Element): List<String> =
        if (element.tagName == "plurals") childElements(element).map { it.textContent } else listOf(element.textContent)

    private fun arguments(text: String): List<String> =
        Regex("%[0-9]+[$][ds]")
            .findAll(text)
            .map {
                it.value
            }.sorted()
            .toList()

    private fun parse(file: File): Element =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement

    private fun childElements(element: Element): List<Element> =
        (0 until element.childNodes.length).mapNotNull { element.childNodes.item(it) as? Element }
}
