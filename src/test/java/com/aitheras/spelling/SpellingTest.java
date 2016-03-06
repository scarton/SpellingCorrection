package com.aitheras.spelling;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Joiner;

/**
 * Unit test for spelling.
 */
public class SpellingTest {
	private static Spelling speller;
	@BeforeClass
	public static void initSpeller() throws IOException {
		speller = new Spelling();
		speller.loadModels();
	}
	@Test
	public void testSpell() {
		String words = "teh quic borwn fxo jumpde voer the lazyz dogg";
		String cwords = Joiner.on(" ").join(speller.correct(words.split(" ")));
//		System.out.println(cwords);
		assertTrue(cwords.equals("the quick brown fox jumped over the lazy dog"));
	}
}
