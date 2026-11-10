
package com.lebaillyapp.proceduralchunkscroller.engine.random

/**
 * # Provides stateless and deterministic pseudo-random values derived from a seed and a discrete index.
 *
 * This object acts as the raw random source for the procedural generation engine.
 *
 * Instead of maintaining an internal random state or storing precomputed values,
 * each sample is calculated independently from its input parameters.
 *
 * The same seed and index will always produce the same result, regardless of:
 * - The order in which samples are requested.
 * - The number of previous samples.
 * - The execution thread.
 * - The number of times a sample is requested.
 *
 * This allows random-access generation without requiring sequential processing
 * or large in-memory datasets.
 *
 * The underlying hash algorithm uses 64-bit integer operations to provide
 * deterministic bit mixing across the entire index domain.
 *
 * No Android, Compose or external random library dependency is required.
 *
 * #### **Compatibility note:**
 * The hashing algorithm and its constants are part of the procedural generation contract.
 *
 * Changing them will alter the generated values for existing seeds and indices,
 * potentially changing previously generated worlds.
 */
object DeterministicSource {

    /**
     * ### Generates a normalized pseudo-random value from a seed and a discrete index.
     *
     * The method first computes a 64-bit hash from the supplied seed and index.
     *
     * It then extracts the upper 53 bits of the resulting hash and converts them
     * into a Double value within the semi-open interval [0.0, 1.0).
     *
     * Using 53 bits matches the significand precision of a Double,
     * avoiding unnecessary precision loss during normalization.
     *
     * The semi-open interval guarantees that:
     * - The minimum possible value is 0.0.
     * - The maximum possible value is strictly less than 1.0.
     *
     * This representation is suitable for interpolation, noise generation
     * and other procedural calculations requiring normalized input values.
     *
     * Each call is independent and does not modify any internal state.
     *
     * @param seed Generation seed identifying a deterministic pseudo-random sequence.
     * @param index Discrete sample index identifying the requested value within that sequence.
     * @return A deterministic pseudo-random Double in the range [0.0, 1.0).
     */
    fun sample01(seed: Long, index: Long): Double {
        val hash = hash64(seed, index)
        val bits = hash ushr 11

        return bits.toDouble() / 9_007_199_254_740_992.0
    }

    /**
     * ### Mixes a seed and a discrete index into a pseudo-random 64-bit hash.
     *
     * The initial XOR combines the seed with an index-dependent value.
     *
     * This ensures that different indices are processed through different
     * input bit patterns.
     *
     * The mixing stage uses a sequence of XOR-shift and multiplication operations
     * based on the 64-bit finalizer constants from MurmurHash3.
     *
     * Each multiplication intentionally relies on JVM Long overflow semantics:
     * only the lower 64 bits of the result are retained.
     *
     * This wrapping arithmetic is an essential part of the hash algorithm,
     * not an arithmetic error.
     *
     * The final XOR-shift further distributes the resulting bits.
     *
     * This method is kept private because the public contract exposes normalized
     * pseudo-random samples rather than the internal hash representation.
     *
     * @param seed Generation seed.
     * @param index Discrete sample index.
     * @return A deterministically mixed 64-bit hash.
     */
    private fun hash64(seed: Long, index: Long): Long {
        var hash = seed xor (index * 0x5555555555555555L)

        hash = (hash xor (hash ushr 33)) * 0xff51afd7ed558ccdUL.toLong()
        hash = (hash xor (hash ushr 33)) * 0xc4ceb9fe1a85ec53UL.toLong()

        return hash xor (hash ushr 33)
    }
}
