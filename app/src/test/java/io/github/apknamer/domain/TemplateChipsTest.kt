package io.github.apknamer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateChipsTest {
    private var counter = 0L
    private fun id() = ++counter
    private fun decode(t: String) = TemplateCodec.decode(t) { id() }

    @Test fun defaultTemplateRoundTrips() {
        assertEquals(NameTemplate.DEFAULT, TemplateCodec.encode(decode(NameTemplate.DEFAULT)))
        assertEquals(NameTemplate.LEGACY_DEFAULT, TemplateCodec.encode(decode(NameTemplate.LEGACY_DEFAULT)))
    }

    @Test fun decodesTokensTextAndGroups() {
        val chips = decode("{name}< V{version}>< ({code})>")
        assertEquals(listOf("name", null, "version", null, "code", null), chips.map { it.token })
        assertEquals(listOf(null, 1, 1, 2, 2, 2), chips.map { it.group })
        assertEquals(" V", chips[1].text)
        assertEquals(" (", chips[3].text)
        assertEquals(")", chips[5].text)
    }

    @Test fun strayMarkersAreIgnored() {
        assertEquals("{name}", TemplateCodec.encode(decode("}{name}>")))
        assertEquals("{name}", TemplateCodec.encode(decode("{name}{}")))
    }

    @Test fun adjacentGroupsStaySeparate() {
        assertEquals("<{a}><{b}>", TemplateCodec.encode(decode("<{a}><{b}>")))
    }

    @Test fun moveReorders() {
        val chips = decode("{a}-{b}-{c}")
        val moved = TemplateEditing.move(chips, 0, 4)
        assertEquals("-{b}-{c}{a}", TemplateCodec.encode(moved))
        assertEquals(chips, TemplateEditing.move(chips, 1, 1))
    }

    @Test fun droppingInsideAGroupJoinsIt() {
        val chips = decode("{x}<{a}{b}>")
        val moved = TemplateEditing.move(chips, 0, 1)
        // x is dropped between a and b, which are both in the same group
        assertEquals("<{a}{x}{b}>", TemplateCodec.encode(moved))
    }

    @Test fun setGroupModes() {
        val chips = decode("{a}{b}")
        val own = TemplateEditing.setGroup(chips, 1, GroupMode.OWN)
        assertEquals("{a}<{b}>", TemplateCodec.encode(own))
        val joined = TemplateEditing.setGroup(chips, 1, GroupMode.WITH_PREVIOUS)
        assertEquals("<{a}{b}>", TemplateCodec.encode(joined))
        val always = TemplateEditing.setGroup(own, 1, GroupMode.ALWAYS)
        assertNull(always[1].group)
    }

    @Test fun cleanTextRemovesSyntaxCharacters() {
        assertEquals("a b", TemplateEditing.cleanText("a<>{ }b"))
    }
}
