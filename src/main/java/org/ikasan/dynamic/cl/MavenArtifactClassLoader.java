package org.ikasan.dynamic.cl;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class loader that can resolve and load classes from Maven artifacts.
 * Takes a Maven coordinate (groupId:artifactId:version) and downloads the artifact
 * along with its dependencies, then binds the classes to the runtime environment.
 */
public class MavenArtifactClassLoader extends URLClassLoader {
    
    private final RepositorySystem repositorySystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;
    
    /**
     * Creates a new MavenArtifactClassLoader with default Maven Central repository.
     */
    public MavenArtifactClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }
    
    /**
     * Creates a new MavenArtifactClassLoader with a parent class loader.
     * 
     * @param parent the parent class loader
     */
    public MavenArtifactClassLoader(ClassLoader parent) {
        super(new URL[0], parent);

        // Initialize repository system
        this.repositorySystem = newRepositorySystem();
        this.session = MavenRepositorySystemUtils.newSession();
        
        // Set local repository (Maven's default location)
        String userHome = System.getProperty("user.home");
        LocalRepository localRepo = new LocalRepository(userHome + "/.m2/repository");
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));
        
        // Add Maven Central as remote repository
        this.repositories = new ArrayList<>();
        repositories.add(new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build());
    }
    
    /**
     * Creates a new RepositorySystem instance.
     *
     * @return configured RepositorySystem
     */
    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Adds an additional remote repository.
     *
     * @param id the repository ID
     * @param url the repository URL
     */
    public void addRepository(String id, String url) {
        repositories.add(new RemoteRepository.Builder(id, "default", url).build());
    }
    
    /**
     * Resolves and loads a Maven artifact and its dependencies.
     * 
     * @param coordinate the Maven coordinate in the format "groupId:artifactId:version"
     * @throws DependencyResolutionException if the artifact cannot be resolved
     */
    public void loadArtifact(String coordinate) throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(coordinate);
        
        // Create dependency and collect request
        Dependency dependency = new Dependency(artifact, "compile");
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(repositories);
        
        // Resolve dependencies
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
        
        // Add all resolved artifacts to the class loader
        for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
            File artifactFile = artifactResult.getArtifact().getFile();
            try {
                addURL(artifactFile.toURI().toURL());
            } catch (Exception e) {
                throw new RuntimeException("Failed to add artifact to classpath: " + artifactFile, e);
            }
        }
    }
    
    /**
     * Resolves and loads a Maven artifact without its dependencies.
     * 
     * @param coordinate the Maven coordinate in the format "groupId:artifactId:version"
     * @throws Exception if the artifact cannot be resolved
     */
    public void loadArtifactOnly(String coordinate) throws Exception {
        Artifact artifact = new DefaultArtifact(coordinate);
        
        // Create a simple artifact request without dependencies
        org.eclipse.aether.resolution.ArtifactRequest request = new org.eclipse.aether.resolution.ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(repositories);
        
        // Resolve the artifact
        org.eclipse.aether.resolution.ArtifactResult result = repositorySystem.resolveArtifact(session, request);
        File artifactFile = result.getArtifact().getFile();
        
        // Add to class loader
        addURL(artifactFile.toURI().toURL());
    }
    
    /**
     * Loads a class from the resolved artifacts.
     * 
     * @param className the fully qualified class name
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    public Class<?> loadClassFromArtifact(String className) throws ClassNotFoundException {
        return loadClass(className);
    }
}
