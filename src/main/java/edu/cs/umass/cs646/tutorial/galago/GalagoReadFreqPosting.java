package edu.cs.umass.cs646.tutorial.galago;

import org.lemurproject.galago.core.index.IndexPartReader;
import org.lemurproject.galago.core.index.KeyIterator;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.retrieval.iterator.CountIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.utility.ByteUtil;

import java.io.File;

/**
 * This is an example for accessing a term-document-frequency posting list from a Galago index.
 *
 * @author Jiepu Jiang (jpjiang@cs.umass.edu)
 * @version 2016-09-10
 */
public class GalagoReadFreqPosting {
	
	public static void main( String[] args ) {
		try {
			
			String pathIndex = "/home/jiepu/Downloads/example_index_galago/";
			
			// Let's just retrieve the posting list for the term "reformulation" in the "text" field
			String field = "text";
			String term = "reformulation";
			
			// by default, the posting list for a field (without using stemming) is stored as a file with the name field.{fieldname}
			File pathPosting = new File( new File( pathIndex ), "field." + field );
			
			// replace the path using the following lines if you hope to access the stemmed (Krovetz or Porter) posting list
			// File pathPosting = new File( new File( pathIndex ), "field.krovetz" + field );
			// File pathPosting = new File( new File( pathIndex ), "field.porter" + field );
			
			// replace the path using the following lines if you hope to access the posting list for the whole document (without considering fields)
			// File pathPosting = new File( new File( pathIndex ), "postings" );
			// File pathPosting = new File( new File( pathIndex ), "postings.krovetz" );
			// File pathPosting = new File( new File( pathIndex ), "postings.porter" );
			
			DiskIndex index = new DiskIndex( pathIndex );
			IndexPartReader posting = DiskIndex.openIndexPart( pathPosting.getAbsolutePath() );
			
			System.out.printf( "%-10s%-15s%-10s\n", "DOCID", "DOCNO", "FREQ" );
			
			KeyIterator vocabulary = posting.getIterator();
			// try to locate the term in the vocabulary
			if ( vocabulary.skipToKey( ByteUtil.fromString( term ) ) && term.equals( vocabulary.getKeyString() ) ) {
				// get an iterator for the term's posting list
				CountIterator iterator = (CountIterator) vocabulary.getValueIterator();
				ScoringContext sc = new ScoringContext();
				while ( !iterator.isDone() ) {
					// Get the current entry's document id.
					// Note that you need to assign the value of sc.document,
					// otherwise count(sc) and others will not work correctly.
					sc.document = iterator.currentCandidate();
					int freq = iterator.count( sc ); // get the frequency of the term in the current document
					String docno = index.getName( sc.document ); // get the docno (external ID) of the current document
					System.out.printf( "%-10s%-15s%-10s\n", sc.document, docno, freq );
					iterator.movePast( iterator.currentCandidate() ); // jump to the entry right after the current one
				}
			}
			
			posting.close();
			index.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
}
