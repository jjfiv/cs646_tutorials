package edu.umass.cs.cs646.tutorial.galago;

import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is an example for accessing a stored document vector from a Galago index.
 *
 * @author Jiepu Jiang (jpjiang@cs.umass.edu)
 * @version 2016-09-10
 */
public class GalagoReadDocVector {
	
	public static void main( String[] args ) {
		try {
			
			String pathIndexBase = "/home/jiepu/Downloads/example_index_galago/";
			
			// Let's just retrieve the document vector (only the "text" field) for the doc with docid=21 from a Galago index.
			String field = "text";
			int docid = 21;
			
			// make sure you stored the corpus when building index (--corpus=true)
			Retrieval retrieval = RetrievalFactory.instance( pathIndexBase );
			Document.DocumentComponents dc = new Document.DocumentComponents( false, false, true );
			
			// now you've retrieved a document stored in the index (including all fields)
			Document doc = retrieval.getDocument( retrieval.getDocumentName( docid ), dc );
			
			// doc.terms will return a list of tokens in the document (including all fields).
			// Now you can iteratively read each token from doc.terms.
			// But you need to construct a document vector representation by yourself.
			
			Map<String, List<Integer>> docVector = new TreeMap<>(); // store term occurrences
			// in order to access a particular field, you need to know where the field's content starts and ends in the document
			for ( Tag tag : doc.tags ) { // doc.tags return a list of document fields
				if ( tag.name.equals( field ) ) { // iteratively checking and find the field you hope to access
					// okay, now keep a copy of the start and end position of the field in the document.
					for ( int position = tag.begin; position < tag.end; position++ ) {
						String term = doc.terms.get( position );
						docVector.putIfAbsent( term, new ArrayList<>() );
						docVector.get( term ).add( position );
					}
				}
			}
			
			// now iteratively print out each term in the document vector and its frequency & positions
			System.out.println( "Document vector representation:" );
			System.out.printf( "%-20s%-10s%-20s\n", "TERM", "FREQ", "POSITIONS" );
			for ( String term : docVector.keySet() ) {
				System.out.printf( "%-20s%-10s", term, docVector.get( term ).size() );
				List<Integer> positions = docVector.get( term );
				for ( int i = 0; i < positions.size(); i++ ) {
					System.out.print( ( i > 0 ? "," : "" ) + positions.get( i ) );
				}
				System.out.println();
			}
			
			retrieval.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
}
