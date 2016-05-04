package enridaga;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.taverna.scufl2.annotation.AnnotationTools;
import org.apache.taverna.scufl2.api.annotation.Annotation;
import org.apache.taverna.scufl2.api.common.Child;
import org.apache.taverna.scufl2.api.common.URITools;
import org.apache.taverna.scufl2.api.common.WorkflowBean;
import org.apache.taverna.scufl2.api.container.WorkflowBundle;
import org.apache.taverna.scufl2.api.core.Workflow;
import org.apache.taverna.scufl2.api.io.ReaderException;
import org.apache.taverna.scufl2.api.io.WorkflowBundleIO;
import org.apache.taverna.scufl2.api.port.InputPort;
import org.apache.taverna.scufl2.ucfpackage.UCFPackage.ResourceEntry;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	private InputStream __f(String n) {
		return getClass().getClassLoader().getResourceAsStream("bundles/" + n + ".wfbundle");
	}

	/**
	 * Tests
	 * 
	 * @throws IOException
	 * @throws ReaderException
	 */
	public void testApp() throws IOException, ReaderException {
		System.out.println("####################");
		// Try reading annotations from 3Drec-v1
		// http://www.myexperiment.org/workflows/4303.html
		WorkflowBundleIO io = new WorkflowBundleIO();
		final WorkflowBundle wb = io.readBundle(__f("3Drec-v1"), null);

		/**
		 * 1. The description of a workflow (as I can see it on the myexperiment
		 * portal)
		 */		
		// TAVERNA-71 workaround
		parseAnnotations(wb);
		
		System.out.println("Annotations: " + wb.getAnnotations().size());
		for (Annotation a : wb.getAnnotations()) {
			System.out.println(a.getRDFContent());
			// Or more elaborate:
			// (in case you want to replace the content you will need the path)
			// String path = getResourcePath();
			// System.out.println(wb.getResources().getResourceAsString(path));
		}

		/**
		 * 2. Annotations of the input ports (we know they have some).
		 */
		AnnotationTools ann = new AnnotationTools();
		Workflow wf = wb.getMainWorkflow();
		// This workflow has 3 main inputs: DataSetPath,Nodes,CoresPerNode
		// We try just the first...
		InputPort inp = wf.getInputPorts().first();
		@SuppressWarnings("unchecked")
		Dataset ds = ann.annotationDatasetFor((Child<InputPort>) inp);
		System.out.println("Print as NQuads:");
		// https://jena.apache.org/documentation/javadoc/arq/org/apache/jena/riot/RDFDataMgr.html
		RDFDataMgr.write(System.out, ds, Lang.NQUADS);
		System.out.println("####################");
		assertTrue(false);
	}

	private void parseAnnotations(final WorkflowBundle wb) throws IOException {
		if (! wb.getAnnotations().isEmpty()) {
			// Assume already parsed
			return;
		}
		URITools uriTools = new URITools();		
		for (ResourceEntry resource : wb.getResources().listResources("annotation").values()) {
			Lang lang = RDFLanguages.contentTypeToLang(resource.getMediaType());
			if (lang == null) {
				// Not a semantic annotation
				continue;
			}
			Annotation ann = new Annotation();
			// Hackish way to generate a name from the annotation filename
			String name = resource.getPath().replace("annotation/", "").replaceAll("\\..*", ""); // strip extension
			ann.setName(name);
			ann.setParent(wb);
			
			String path = resource.getPath();
			ann.setBody(URI.create("/" + path));			
			//System.out.println(path);			
			URI base = wb.getGlobalBaseURI().resolve(path);
			Model model = ModelFactory.createDefaultModel();			
			RDFDataMgr.read(model, resource.getUcfPackage().getResourceAsInputStream(path), 
					base.toASCIIString(), lang);
			ResIterator subjs = model.listSubjects();			
			while (subjs.hasNext()) { 
				Resource r = subjs.next();
				//System.out.println(r);
				WorkflowBean b = uriTools.resolveUri(URI.create(r.getURI()), wb);
				//System.out.println(b);
				if (b != null) {
					ann.setTarget(b);
				}
				break;
			}
			// TODO: Could just dump the nquads directly from here
		}
	}
}
