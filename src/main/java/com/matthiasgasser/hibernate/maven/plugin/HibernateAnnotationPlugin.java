package com.matthiasgasser.hibernate.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Goal which adds hibernate contribution (@Entity annotated classes) to the
 * manifest file
 * 
 * @author matthias gasser
 * 
 * goal hibernate
 * 
 * phase process-sources
 * 
 * @goal bundle
 * @phase package
 * 
 */
public class HibernateAnnotationPlugin extends AbstractMojo {

	public static final String ENTITY_ANNOTATION = "Entity";

	/**
	 * Directory where the manifest will be written
	 * 
	 * @parameter expression="${manifestLocation}"
	 *            default-value="${project.build.outputDirectory}/META-INF"
	 */
	protected File manifestLocation;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Defines files in the source directories to include (all .java files by
	 * default).
	 * 
	 * @parameter
	 */
	private String[] includes = { "**/*.java" };

	/**
	 * Defines which of the included files in the source directories to exclude
	 * (non by default).
	 * 
	 * @parameter
	 */
	private String[] excludes;
	
	/**
     * The directory for the generated bundles.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File buildDirectory;
    
    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * The Maven PluginManager Object
     *
     * @component
     * @required
     */
    protected BuildPluginManager pluginManager;
    

	public void execute() throws MojoExecutionException {

		try {
		
			List<String> entities = buildEntityClassList();
			getLog().info("Found " + entities.size() + " entity classes.");
			
			// appendHibernateToManifest(entities);
			// getLog().info("Wrote " + entities.size() + " entities to Manifest at " + manifestLocation.getCanonicalPath());
		
			updateJar(entities);
			getLog().info("Updated Jar");
		
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("File not found: " + e.getLocalizedMessage());
		} catch (ParseException e) {
			throw new MojoExecutionException("Parser Exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			throw new MojoExecutionException("IO Exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * calls the maven jar plugin and forces a recreation of the already built (by bnd) jar, 
	 * but with an extra hibernate contribution section
	 * @param entities
	 * @throws MojoExecutionException
	 * @throws IOException
	 */
	private void updateJar(List<String> entities) throws MojoExecutionException, IOException {
		executeMojo(
				plugin(
			    		groupId("org.apache.maven.plugins"),
			    		artifactId("maven-jar-plugin"),
			    		version("2.3.2")
			        ),
			        goal("jar"),
			        configuration(
			        		element(name("forceCreation"),"true"),
			        		element(name("outputDirectory"), buildDirectory.getAbsolutePath()),
			        		element(name("archive"),
			        				element(name("manifestFile"),manifestLocation.getCanonicalPath() + "/MANIFEST.MF"),
			        				element(name("manifestEntries"),
			        						element(name("Hibernate-Contribution"),entitiesToString(entities))
			        				)
			        		)
			        ),
			        executionEnvironment(
			            project,
			            session,
			            pluginManager
			        )
			    );
	}

	private List<String> buildEntityClassList() throws FileNotFoundException, ParseException, IOException {
		List<String> entities = new ArrayList<String>();

		List<String> srcDirs = project.getCompileSourceRoots();

		for (String src : srcDirs) {
			File root = new File(src);
			final DirectoryScanner directoryScanner = new DirectoryScanner();
			directoryScanner.setIncludes(includes);
			directoryScanner.setExcludes(excludes);
			directoryScanner.setBasedir(root);
			directoryScanner.scan();

			for (String fileName : directoryScanner.getIncludedFiles()) {
				final File file = new File(root, fileName);

				if (file != null) {
					getLog().info("Parsing: " + fileName);

					InspectedFile iFile = inspectJavaFileForEntitiyAnnotation(file);
					if (iFile.isEntityClass()) {

						entities.add(iFile.getCanonicalName());
					}
				}
			}
		}
		return entities;
	}

//	private void appendHibernateToManifest(List<String> entities) throws IOException {
//		RandomAccessFile file = new RandomAccessFile(manifestLocation.getCanonicalPath() + "/MANIFEST.MF", "rw");
//		
//		FileWriter fstream = new FileWriter(manifestLocation.getCanonicalPath() + "/MANIFEST.MF", true);
//		BufferedWriter out = new BufferedWriter(fstream);
//		try {
//			out.write("Hibernate-Contribution: default; classes=\"");
//			for (String clazz : entities) {
//				out.write(clazz + ",");
//			}
//			out.write("\"\n\n");
//		} finally {
//			out.close();
//		}
//	}

	private static String entitiesToString(List<String> entities) {
		StringBuffer buffer = new StringBuffer("default; classes=\"");
		Iterator<String> iterator = entities.iterator();
		while(iterator.hasNext()) {
			buffer.append(iterator.next());
			if(iterator.hasNext())
				buffer.append(",");
		}
		buffer.append("\"");
		return buffer.toString();
	}
	
	/**
	 * 
	 * @param pFile
	 *            file to inspect
	 * @return a filed InspectedFile object
	 * @throws FileNotFoundException
	 * @throws ParseException
	 * @throws IOException
	 */
	private InspectedFile inspectJavaFileForEntitiyAnnotation(File pFile) throws FileNotFoundException, ParseException, IOException {
		CompilationUnit cu;
		FileInputStream in = new FileInputStream(pFile);
		try {
			cu = JavaParser.parse(in);
		} finally {
			in.close();
		}

		List<String> list = new ArrayList<String>();
		new ClassVisitor().visit(cu, list);

		// quick'n'dirty, build up the canonical class name and create a new
		// InspectedFile
		return new InspectedFile(list, cu.getPackage().getName() + "." + pFile.getName().substring(0, pFile.getName().indexOf(".java")));

	}

	/**
	 * @author matthias visits every class and fills the argument list with the
	 *         class annotations as Strings
	 */
	private static class ClassVisitor extends VoidVisitorAdapter<List<String>> {
		@Override
		public void visit(ClassOrInterfaceDeclaration n, List<String> arg) {
			if (n.getAnnotations() != null) {
				for (AnnotationExpr ann : n.getAnnotations()) {
					if (ann != null && ann.getName() != null)
						arg.add(ann.getName().toString());
				}
			}
		}
	}
}
