package com.blackducksoftware.integration.hub.detect.bomtool.npm

import java.nio.file.Paths

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.detect.model.DetectCodeLocation
import com.blackducksoftware.integration.hub.detect.testutils.DependencyGraphResourceTestUtil
import com.blackducksoftware.integration.hub.detect.testutils.TestUtil

public class NpmOutputParserTest {
    private TestUtil testUtil = new TestUtil()

    @Test
    public void npmCliDependencyFinder() throws IOException {
        def parser = new NpmCliDependencyFinder()
        parser.externalIdFactory = new ExternalIdFactory()
        String testIn = testUtil.getResourceAsUTF8String('/npm/packman_proj_dependencies.json')
        DetectCodeLocation codeLocation = parser.convertNpmJsonFileToCodeLocation(Paths.get('source'), testIn)

        Assert.assertEquals(codeLocation.bomToolProjectName, 'node-js')
        Assert.assertEquals(codeLocation.bomToolProjectVersionName, '0.2.0')
        DependencyGraphResourceTestUtil.assertGraph('/npm/npmParseOutput_graph.json', codeLocation.dependencyGraph)
    }
}
