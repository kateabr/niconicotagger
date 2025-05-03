package niconicotagger.datagen

import org.instancio.Node
import org.instancio.Random
import org.instancio.generator.Generator
import org.instancio.generator.GeneratorSpec
import org.instancio.generators.Generators
import org.instancio.internal.nodes.InternalNode
import org.instancio.internal.reflect.ParameterizedTypeImpl
import org.instancio.spi.InstancioServiceProvider
import org.springframework.util.MultiValueMap

class CustomServiceProvider : InstancioServiceProvider {
    override fun getGeneratorProvider(): InstancioServiceProvider.GeneratorProvider {
        return CustomGeneratorProvider()
    }

    class CustomGeneratorProvider : InstancioServiceProvider.GeneratorProvider {
        override fun getGenerator(node: Node?, generators: Generators): GeneratorSpec<*>? {
            if (node?.let { node as InternalNode }?.type == MultiValueMapGenerator.multiValueStringMapType) {
                return MultiValueMapGenerator()
            }

            return null
        }
    }

    class MultiValueMapGenerator : Generator<MultiValueMap<String, String>> {
        override fun generate(random: Random): MultiValueMap<String, String> {
            return MultiValueMap.fromMultiValue(
                mapOf(
                    random.alphanumeric(10) to
                        generateSequence<Long>(0) { it + 1 }
                            .take(random.intRange(1, 4))
                            .map { random.alphanumeric(10) }
                            .toList()
                )
            )
        }

        companion object {
            val multiValueStringMapType =
                ParameterizedTypeImpl(MultiValueMap::class.java, String::class.java, String::class.java)
        }
    }
}
