package com.example.smartassistant.command

import com.example.smartassistant.R
import com.example.smartassistant.i18n.StringProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Перевіряє парсер без Android: ключові фрази підставляє підроблений [StringProvider]. */
class CommandParserTest {

    private val keywords = mapOf(
        R.array.kw_greeting to listOf("hello", "hi", "good morning"),
        R.array.kw_time to listOf("what time", "current time"),
        R.array.kw_date to listOf("what date", "what day"),
        R.array.kw_battery to listOf("battery"),
        R.array.kw_camera to listOf("open camera", "take photo"),
        R.array.kw_timer to listOf("timer"),
        R.array.kw_unit_hour to listOf("hour", "hours"),
        R.array.kw_search to listOf("search the web for", "search for", "look up", "google"),
        R.array.kw_help to listOf("help"),
    )

    private val fakeStrings = object : StringProvider {
        override fun getString(id: Int, vararg args: Any): String = error("not needed in parser tests")
        override fun getQuantityString(id: Int, quantity: Int, vararg args: Any): String = error("not needed")
        override fun getStringArray(id: Int): List<String> = keywords.getValue(id)
        override fun locale(): Locale = Locale.ENGLISH
    }

    private val parser = CommandParser(fakeStrings)

    @Test
    fun `recognizes greeting`() = assertEquals(Command.Greeting, parser.parse("Hi there"))

    @Test
    fun `recognizes time question regardless of case and punctuation`() =
        assertEquals(Command.Time, parser.parse("What time is it?"))

    @Test
    fun `recognizes date question`() = assertEquals(Command.Date, parser.parse("what day is it today"))

    @Test
    fun `recognizes battery`() = assertEquals(Command.Battery, parser.parse("how is my battery"))

    @Test
    fun `recognizes camera`() = assertEquals(Command.OpenCamera, parser.parse("please open the camera"))

    @Test
    fun `recognizes help`() = assertEquals(Command.Help, parser.parse("help"))

    @Test
    fun `timer in minutes`() = assertEquals(Command.SetTimer(5), parser.parse("set a timer for 5 minutes"))

    @Test
    fun `timer in hours is converted to minutes`() =
        assertEquals(Command.SetTimer(120), parser.parse("Set a timer for 2 hours"))

    @Test
    fun `timer without duration asks for it`() = assertEquals(Command.SetTimer(null), parser.parse("set a timer"))

    @Test
    fun `search extracts the query after the trigger`() =
        assertEquals(Command.WebSearch("funny cats"), parser.parse("Search for funny cats"))

    @Test
    fun `search has priority over other commands inside the query`() =
        assertEquals(Command.WebSearch("battery tips"), parser.parse("search for battery tips"))

    @Test
    fun `search without query`() = assertEquals(Command.WebSearch(null), parser.parse("google"))

    @Test
    fun `unknown text keeps the original phrase`() =
        assertEquals(Command.Unknown("blah blah"), parser.parse("  blah blah "))
}
