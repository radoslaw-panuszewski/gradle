/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.component.external.model

import org.gradle.api.Action
import org.gradle.api.artifacts.DependenciesMetadata
import org.gradle.api.internal.artifacts.DefaultImmutableModuleIdentifierFactory
import org.gradle.api.internal.artifacts.repositories.metadata.MavenMutableModuleMetadataFactory
import org.gradle.internal.component.external.descriptor.MavenScope
import org.gradle.util.TestUtil

import static org.gradle.internal.component.external.model.DefaultModuleComponentSelector.newSelector

class DependencyConstraintMetadataRulesTest extends AbstractDependencyMetadataRulesTest {
    private final mavenMetadataFactory = new MavenMutableModuleMetadataFactory(new DefaultImmutableModuleIdentifierFactory(), TestUtil.attributesFactory(), TestUtil.objectInstantiator(), TestUtil.experimentalFeatures())

    @Override
    boolean addAllDependenciesAsConstraints() {
        return true
    }

    @Override
    void doAddDependencyMetadataRule(MutableModuleComponentResolveMetadata metadataImplementation, String variantName, Action<? super DependenciesMetadata> action) {
        metadataImplementation.addDependencyConstraintMetadataRule(variantName, action, instantiator, notationParser, constraintNotationParser)
    }

    def "maven optional dependencies are accessible as dependency constraints"() {
        given:
        def mavenMetadata = mavenMetadataFactory.create(componentIdentifier, [
            new MavenDependencyDescriptor(MavenScope.Compile, false, newSelector("org", "notOptional", "1.0"), null, []),
            new MavenDependencyDescriptor(MavenScope.Compile, true, newSelector("org", "optional", "1.0"), null, [])
        ])

        when:
        mavenMetadata.addDependencyMetadataRule("default", {
            assert it.size() == 1
            assert it[0].name == "notOptional"
        }, instantiator, notationParser, constraintNotationParser)
        mavenMetadata.addDependencyConstraintMetadataRule("default", {
            assert it.size() == 1
            assert it[0].name == "optional"
        }, instantiator, notationParser, constraintNotationParser)

        then:
        def dependencies = selectTargetConfigurationMetadata(mavenMetadata).dependencies
        dependencies.size() == 2
        !dependencies[0].pending
        dependencies[1].pending
    }
}
