package de.foodplanner.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTextTest {

    @Test
    void foldLowercasesAndStripsAccents() {
        assertThat(SearchText.fold("Caffè")).isEqualTo("caffe");
        assertThat(SearchText.fold("CRÈME Brûlée")).isEqualTo("creme brulee");
    }

    @Test
    void foldMapsSharpS() {
        assertThat(SearchText.fold("Weißbier")).isEqualTo("weissbier");
    }

    @Test
    void tokensSplitOnPunctuationAndWhitespace() {
        assertThat(SearchText.parse("Fettarme Milch 1,5 %").tokens()).containsExactly("fettarme", "milch", "1", "5");
    }

    @Test
    void tokensAreDistinctInOrder() {
        assertThat(SearchText.parse("milch Milch fettarm").tokens()).containsExactly("milch", "fettarm");
    }

    @Test
    void onlyPunctuationIsEmpty() {
        assertThat(SearchText.parse(" %, - ").isEmpty()).isTrue();
        assertThat(SearchText.parse("").isEmpty()).isTrue();
    }

    @Test
    void specialCharactersNeverReachTokens() {
        assertThat(SearchText.parse("o'neil c++ 50% (x) \\").tokens()).containsExactly("o", "neil", "c", "50", "x");
    }
}
