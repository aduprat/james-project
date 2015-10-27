package org.apache.james.rrt.lib;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;


public class MappingsImplTest {

    @Test(expected=NullPointerException.class)
    public void fromRawStringShouldThrowWhenNull() {
        MappingsImpl.fromRawString(null);
    }

    @Test
    public void fromRawStringShouldReturnEmptyCollectionWhenEmptyString() {
        MappingsImpl actual = MappingsImpl.fromRawString("");
        assertThat(actual).isEmpty();
    }
    
    @Test
    public void fromRawStringShouldReturnSingletonCollectionWhenSingleElementString() {
        MappingsImpl actual = MappingsImpl.fromRawString("value");
        assertThat(actual).containsExactly("value");
    }

    @Test
    public void fromRawStringShouldReturnCollectionWhenSeveralElementsString() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1;value2");
        assertThat(actual).containsExactly("value1", "value2");
    }
    
    @Test
    public void fromRawStringShouldReturnSingleElementCollectionWhenTrailingDelimiterString() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1;");
        assertThat(actual).containsExactly("value1");
    }

    @Test
    public void fromRawStringShouldReturnSingleElementCollectionWhenHeadingDelimiterString() {
        MappingsImpl actual = MappingsImpl.fromRawString(";value1");
        assertThat(actual).containsExactly("value1");
    }
    

    @Test
    public void fromRawStringShouldTrimValues() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1 ; value2  ");
        assertThat(actual).containsExactly("value1", "value2");
    }
    
    @Test
    public void fromRawStringShouldNotSkipEmptyValue() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1; ;value2");
        assertThat(actual).containsExactly("value1", "", "value2");
    }
    
    @Test
    public void fromRawStringShouldReturnCollectionWhenValueContainsCommaSeperatedValues() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1,value2");
        assertThat(actual).containsExactly("value1","value2");
    }

    @Test
    public void fromRawStringShouldReturnCollectionWhenValueContainsColonSeperatedValues() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1:value2");
        assertThat(actual).containsExactly("value1","value2");
    }

    @Test
    public void fromRawStringShouldUseCommaDelimiterBeforeSemicolonWhenValueContainsBoth() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1;value1,value2");
        assertThat(actual).containsExactly("value1;value1","value2");
    }

    @Test
    public void fromRawStringShouldUseSemicolonDelimiterBeforeColonWhenValueContainsBoth() {
        MappingsImpl actual = MappingsImpl.fromRawString("value1:value1;value2");
        assertThat(actual).containsExactly("value1:value1","value2");
    }
    
    @Test
    public void fromRawStringShouldNotUseColonDelimiterWhenValueStartsWithError() {
        MappingsImpl actual = MappingsImpl.fromRawString("error:test");
        assertThat(actual).containsExactly("error:test");
    }
    

    @Test
    public void fromRawStringShouldNotUseColonDelimiterWhenValueStartsWithDomain() {
        MappingsImpl actual = MappingsImpl.fromRawString("domain:test");
        assertThat(actual).containsExactly("domain:test");
    }
    

    @Test
    public void fromRawStringShouldNotUseColonDelimiterWhenValueStartsWithRegex() {
        MappingsImpl actual = MappingsImpl.fromRawString("regex:test");
        assertThat(actual).containsExactly("regex:test");
    }

    
}
