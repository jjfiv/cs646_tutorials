package edu.umass.cs.cs646.tutorial.galago;

import org.lemurproject.galago.core.index.LengthsReader;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.index.disk.DiskLengthsReader;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.iterator.LengthsIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;

import java.io.File;

/**
 * This is an example of counting document field length in Galago.
 *
 * @author Jiepu Jiang (jpjiang@cs.umass.edu)
 * @version 2016-09-10
 */
public class GalagoReadDocLength {
	
	public static void main( String[] args ) {
		try {
			
			String pathIndexBase = "/home/jiepu/Downloads/example_index_galago/";
			String field = "text";
			
			// by default, document length information is stored in a file named "lengths" under the index directory
			File fileLength = new File( new File( pathIndexBase ), "lengths" );
			
			Retrieval retrieval = RetrievalFactory.instance( pathIndexBase );
			
			LengthsReader indexLength = new DiskLengthsReader( fileLength.getAbsolutePath() );
			Node query = StructuredQuery.parse( "#lengths:" + field + ":part=lengths()" );
			
			System.out.printf( "%-10s%-15s%-10s\n", "DOCID", "DOCNO", "Length" );
			LengthsIterator iterator = (LengthsIterator) indexLength.getIterator( query );
			ScoringContext sc = new ScoringContext();
			while ( !iterator.isDone() ) {
				sc.document = iterator.currentCandidate();
				String docno = retrieval.getDocumentName( (int) sc.document );
				int length = iterator.length( sc );
				System.out.printf( "%-10d%-15s%-10d\n", sc.document, docno, length );
				iterator.movePast( iterator.currentCandidate() );
			}
			
			indexLength.close();
			retrieval.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
}
