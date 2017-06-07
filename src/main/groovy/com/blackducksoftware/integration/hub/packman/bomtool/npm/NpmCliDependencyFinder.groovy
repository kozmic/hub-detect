package com.blackducksoftware.integration.hub.packman.bomtool.npm

import java.io.File

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge
import com.blackducksoftware.integration.hub.packman.util.executable.Executable
import com.blackducksoftware.integration.hub.packman.util.executable.ExecutableRunner

import com.google.gson.Gson
import com.google.gson.stream.JsonReader

@Component
class NpmCliDependencyFinder {
	
	@Autowired
	Gson gson
	
	public DependencyNode generateDependencyNode(String rootDirectoryPath, String exePath) {
		DependencyNode result;
		
		def npmLsExe = new Executable(new File(rootDirectoryPath), exePath, ['ls', '-json'])
		def exeRunner = new ExecutableRunner()
		def tempJsonOutFile = new File('packman_proj_dependencies.json')
		def cliOut = exeRunner.executeQuietly(npmLsExe)
		
		//Here I can output an error message somewhere
		if(cliOut.errorOutput) {
			return null
		}
		
		tempJsonOutFile.newWriter().withWriter { w ->
			w << cliOut.standardOutput
		}
		
		if(tempJsonOutFile) {
			result = convertNpmJsonFileToDependencyNode(tempJsonOutFile)
		
			try {
				tempJsonOutFile.delete()
			} catch (IOException e) {
				println(e.stackTrace)
			}
			
			
		}
		
		println('Finished')
		
		result
	}
	
	private DependencyNode convertNpmJsonFileToDependencyNode(File file) {
		convertToDependencyNode(jsonDeserialization(file), null)
	}
	
	private NpmCliNode jsonDeserialization(File depOut) {
		gson.fromJson(new JsonReader(new FileReader(depOut)), NpmCliNode.class)
	}
	
	private DependencyNode convertToDependencyNode(NpmCliNode node, String keyName) {
		def name = node.name
		
		//If a keyname is provided through recursion, use that name instead of the node name (Because of package.json structure)
		if(keyName) {
			name = keyName
		}
		
		def version = node.version
		def externalId = new NameVersionExternalId(Forge.NPM, name, version)
		def dependencyNode = new DependencyNode(name, version, externalId)
		node.dependencies.each {
			dependencyNode.children.add(convertToDependencyNode(it.getValue(), it.getKey()))
		}
		
		dependencyNode
	}
	
	static main (args) {
		def path = '/usr/local/bin/npm'
		def parser = new NpmCliDependencyFinder()
		//def bigTest = parser.parseNpm('/Users/bmandel/Node/NodeProj', path)
		def littleTest = parser.generateDependencyNode('/Users/bmandel/Documents/NodeJS', path)
		
		println(littleTest.toString())
	}
}
