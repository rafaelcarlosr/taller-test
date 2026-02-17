package com.taller.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ProductExceptSelfTest {

    private ProductExceptSelf productExceptSelf;

    @BeforeEach
    void setUp() {
        productExceptSelf = new ProductExceptSelf();
    }


    /**
     * Test when the input array contains all positive integers.
     */
    @Test
    void testAllPositiveNumbers() {
        int[] input = {1, 2, 3, 4};
        int[] expected = {24, 12, 8, 6};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }

    /**
     * Test when the input array contains some zero values.
     */
    @Test
    void testArrayWithZeros() {
        int[] input = {1, 2, 0, 4};
        int[] expected = {0, 0, 8, 0};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }

    /**
     * Test when the input array contains all zeros.
     */
    @Test
    void testAllZeros() {
        int[] input = {0, 0, 0};
        int[] expected = {0, 0, 0};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }

    /**
     * Test when the input array contains negative integers.
     */
    @Test
    void testNegativeNumbers() {
        int[] input = {-1, -2, -3, -4};
        int[] expected = {-24, -12, -8, -6};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }

    /**
     * Test when the input array contains a mix of positive and negative numbers.
     */
    @Test
    void testMixedPositiveAndNegativeNumbers() {
        int[] input = {1, -2, 3, -4};
        int[] expected = {24, -12, 8, -6};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }

    /**
     * Test when the input array contains only a single element.
     */
    @Test
    void testSingleElementArray() {
        int[] input = {5};
        int[] expected = {1};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }

    /**
     * Test when the input array contains two elements.
     */
    @Test
    void testTwoElementArray() {
        int[] input = {3, 7};
        int[] expected = {7, 3};
        assertArrayEquals(expected, productExceptSelf.productExceptSelf(input));
    }
}