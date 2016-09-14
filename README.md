
## UMASS CS646 Information Retrieval (Fall 2016)
## A Simple Tutorial of Galago and Lucene (for CS646 Students)

_Last Update: 9/13/2016_

A quick index:
* [Installation](https://github.com/jiepujiang/cs646_tutorials#installation)
* [Build an Index](https://github.com/jiepujiang/cs646_tutorials#build-an-index)
* [Working with an Index](https://github.com/jiepujiang/cs646_tutorials#working-with-an-index)
    * [External and Internal IDs](https://github.com/jiepujiang/cs646_tutorials#external-and-internal-ids)
    * [Frequency Posting List](https://github.com/jiepujiang/cs646_tutorials#frequency-posting-list)
    * [Position Posting List](https://github.com/jiepujiang/cs646_tutorials#position-posting-list)
    * [Accessing an Indexed Document](https://github.com/jiepujiang/cs646_tutorials#accessing-an-indexed-document)
    * [Document and Field Length](https://github.com/jiepujiang/cs646_tutorials#document-and-field-length)
    * [Corpus-level Statistics](https://github.com/jiepujiang/cs646_tutorials#corpus-level-statistics)
* [Searching](https://github.com/jiepujiang/cs646_tutorials#searching)

## Environment

This tutorial uses:
* Oracle JDK 1.8
* Galago 3.10
* Lucene 6.2

## Installation

### Galago

Galago is an information retrieval system written in Java for research use.

Download and decompress ```galago-3.10-bin.tar.gz``` from https://sourceforge.net/projects/lemur/files/lemur/galago-3.10/.
Galago has a command line tool ```{your galago folder}/bin/galago```, which has many useful functionalities. 

To use Galago in your project, you need to include all the jar files under Galago's lib folder into your project
(well, you may not necessarily need ALL of them, but just in case you miss something).

You may find it helpful to append Galago's bin folder to your system path such that you can ```galago``` anywhere in your command line.

You can also build Galago by your own (using Maven). Just download and decompress ```galago-3.10.tar.gz``` and follow the instructions in ```BUILD```.

Note that the Galago we are using is different from the one described in the WBC textbook.

Support:
* Official tutorial: https://sourceforge.net/p/lemur/wiki/Galago/
* Discussion forum: https://sourceforge.net/p/lemur/discussion/
* Bug report: https://sourceforge.net/p/lemur/bugs/

### Lucene

Apache Lucene is an information retrieval system written in Java (but not for the purpose of IR research).

The easiest way to use Lucene in your project is to import it using Maven.
You need to at least import ```lucene-core``` (just pasting the following to your ```pom.xml```'s dependencies).

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version>6.2.0</version>
</dependency>
```

You may also need ```lucene-analyzers-common``` and ```lucene-queryparser``` as well.

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-analyzers-common</artifactId>
    <version>6.2.0</version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-queryparser</artifactId>
    <version>6.2.0</version>
</dependency>
```

If you do not use Maven, you need to download the jar files by yourself and include them into your project.
Make sure you download the correct version.
http://archive.apache.org/dist/lucene/java/6.2.0/

Support:
* Official documentation: http://lucene.apache.org/core/6_2_0/

## Build an Index

### Corpus

This tutorial uses a small trectext format corpus.
You can download the corpus at https://github.com/jiepujiang/cs646_tutorials/blob/master/example_corpus.gz

The corpus includes the information of about 100 articles published by CIIR scholars in SIGIR conferences.
Each document is in the following format:

```xml
<DOC>
<DOCNO>ACM-383972</DOCNO>
<TITLE>Relevance based language models</TITLE>
<AUTHOR>Victor Lavrenko, W. Bruce Croft</AUTHOR>
<SOURCE>Proceedings of the 24th annual international ACM SIGIR conference on Research and development in information retrieval</SOURCE>
<TEXT>
We explore the relation between classical probabilistic models of information retrieval and the emerging language modeling approaches. It has long been recognized that the primary obstacle to effective performance of classical models is the need to estimate a relevance model : probabilities of words in the relevant class. We propose a novel technique for estimating these probabilities using the query alone. We demonstrate that our technique can produce highly accurate relevance models, addressing important notions of synonymy and polysemy. Our experiments show relevance models outperforming baseline language modeling systems on TREC retrieval and TDT tracking tasks. The main contribution of this work is an effective formal method for estimating a relevance model with no training data.
</TEXT>
</DOC>
```

A document has five fields.
The DOCNO field specifies a unique ID (docno) for each article.
We need to build an index for the other four text fields such that we can retrieve the documents.

### Text Processing and Indexing Options

Many IR systems may require you to specify a few text processing options for indexing:
* **Tokenization** -- how to split a sequence of text into individual tokens (most tokens are just words).
The default tokenization option is usually enough for normal English-language text retrieval applications.
* **Letter case** -- Most IR systems ignore letter case differences between tokens/words. 
But sometimes they may be important, e.g., **smart** and **SMART** (the SMART retrieval system). 
* **Stop words** -- You may want to remove some stop words such as **is**, **the**, and **to**. 
Removing stop words can significantly reduce index size. 
But it may also cause problems for some queries such as ```to be or not to be```.
We recommend you to keep them unless you cannot afford a larger index.
* **Stemming** -- You may want to index stemmed words rather than the original ones to ignore minor word differences such as **model** vs. **models**.
Stemming is not perfect and may get wrong. IR systems often use **Porter**, **Porter2**, or **Krovetz** stemming. Just a few examples for their differences:

Original    | Porter2   | Krovetz
--------    | -------   | -------
relevance   | relev     | relevance
based       | base      | base
language    | languag   | language
models      | model     | model

An indexed document can have different fields to store different types of information. 
Most IR systems support two types of fields:
* **Metadata field** is similar to a database record's field. 
They are stored and indexed as a whole without tokenization.
It is suitable for data such as IDs (such as the docno field in our example corpus).
Some IR systems support storing and indexing numeric values 
(and you can search for indexed numeric values using range or greater-than/less-than queries).
* **Normal text field** is suitable for regular text contents (such as the other four fields in our example corpus).
The texts are tokenized and indexed (using inverted index), such that you can search using normal text retrieval techniques.

### Galago

Galago supports many corpus formats widely used in the IR community such as trectext, trecweb, warc, etc.

Galago has a command line tool for building index.
You can get help information by running:
```
{you galago path}/bin/galago build
```

To build an index for the example corpus, simply run:
```
{you galago path}/bin/galago build --indexPath={path of your index} --inputPath+{path of example_corpus.gz} --nonStemmedPostings=true --stemmedPostings=true --stemmer+krovetz --corpus=true --tokenizer/fields+title --tokenizer/fields+author --tokenizer/fields+source --tokenizer/fields+text
```

Note that for trectext format corpus, Galago will automatically store and index the docno field as metadata. 

If the corpus is large, it will take Galago some time to finish the index.
You can monitor the progress by appending the following parameters to ```galago build```
(you can replace ```60000``` with any unused port numbers on your machine):
```
--server=true --port=60000
```

While Galago is building index, you can check the progress in a browser by visiting http://localhost:6000

You can download the Galago index for the example corpus at https://github.com/jiepujiang/cs646_tutorials/blob/master/example_index_galago.tar.gz

### Lucene

Lucene does not have a command line tool for indexing (as far as I know).
You need to write your own program to build an index.
This is the one I used for the example corpus.
```
String pathCorpus = "/home/jiepu/Downloads/example_corpus.gz";
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

// Analyzer includes options for text processing
Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents( String fieldName ) {
        // Step 1: tokenization (Lucene's StandardTokenizer is suitable for most text retrieval occasions)
        TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
        // Step 2: transforming all tokens into lowercased ones
        ts = new TokenStreamComponents( ts.getTokenizer(), new LowerCaseFilter( ts.getTokenStream() ) );
        // Step 3: whether to remove stop words
        // Uncomment the following line to remove stop words
        // ts = new TokenStreamComponents( ts.getTokenizer(), new StopwordsFilter( ts.getTokenStream(), StandardAnalyzer.ENGLISH_STOP_WORDS_SET ) );
        // Step 4: whether to apply stemming
        // Uncomment the following line to apply Krovetz or Porter stemmer
        // ts = new TokenStreamComponents( ts.getTokenizer(), new KStemFilter( ts.getTokenStream() ) );
        // ts = new TokenStreamComponents( ts.getTokenizer(), new PorterStemFilter( ts.getTokenStream() ) );
        return ts;
    }
};
// Read more about Lucene's text analysis: http://lucene.apache.org/core/6_2_0/core/org/apache/lucene/analysis/package-summary.html#package.description

IndexWriterConfig config = new IndexWriterConfig( analyzer );
// Note that IndexWriterConfig.OpenMode.CREATE will override the original index in the folder
config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );

IndexWriter ixwriter = new IndexWriter( dir, config );

// This is the field setting for metadata field.
FieldType fieldTypeMetadata = new FieldType();
fieldTypeMetadata.setOmitNorms( true );
fieldTypeMetadata.setIndexOptions( IndexOptions.DOCS );
fieldTypeMetadata.setStored( true );
fieldTypeMetadata.setTokenized( false );
fieldTypeMetadata.freeze();

// This is the field setting for normal text field.
FieldType fieldTypeText = new FieldType();
fieldTypeText.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
fieldTypeText.setStoreTermVectors( true );
fieldTypeText.setStoreTermVectorPositions( true );
fieldTypeText.setTokenized( true );
fieldTypeText.setStored( true );
fieldTypeText.freeze();

// You need to iteratively read each document from the corpus file,
// create a Document object for the parsed document, and add that
// Document object by calling addDocument().

// Well, the following only works for small text files. DO NOT follow this part in your homework!
InputStream instream = new GZIPInputStream( new FileInputStream( pathCorpus ) );
String corpusText = new String( IOUtils.toByteArray( instream ), "UTF-8" );
instream.close();

Pattern pattern = Pattern.compile(
        "<DOC>.+?<DOCNO>(.+?)</DOCNO>.+?<TITLE>(.+?)</TITLE>.+?<AUTHOR>(.+?)</AUTHOR>.+?<SOURCE>(.+?)</SOURCE>.+?<TEXT>(.+?)</TEXT>.+?</DOC>",
        Pattern.CASE_INSENSITIVE + Pattern.MULTILINE + Pattern.DOTALL
);

Matcher matcher = pattern.matcher( corpusText );

while ( matcher.find() ) {
    
    String docno = matcher.group( 1 ).trim();
    String title = matcher.group( 2 ).trim();
    String author = matcher.group( 3 ).trim();
    String source = matcher.group( 4 ).trim();
    String text = matcher.group( 5 ).trim();
    
    // Create a Document object
    Document d = new Document();
    // Add each field to the document with the appropriate field type options
    d.add( new Field( "docno", docno, fieldTypeMetadata ) );
    d.add( new Field( "title", title, fieldTypeText ) );
    d.add( new Field( "author", author, fieldTypeText ) );
    d.add( new Field( "source", source, fieldTypeText ) );
    d.add( new Field( "text", text, fieldTypeText ) );
    // Add the document to index.
    ixwriter.addDocument( d );
}

// remember to close both the index writer and the directory
ixwriter.close();
dir.close();
```

You can download the Lucene index for the example corpus at https://github.com/jiepujiang/cs646_tutorials/blob/master/example_index_lucene.tar.gz

## Working with an Index

### Openning and Closing an Index

#### Galago

You will find the following files in the Galago index folder. 
Each file stores a part of the index.
* ```postings``` -- the unstemmed inverted index for the whole document.
* ```postings.krovetz``` or ```postings.porter``` -- the stemmed inverted index for the whole document.
* ```field.{fieldName}``` -- the unstemmed inverted index for a particular field.
* ```field.krovetz.{fieldName}``` or ```field.porter.{fieldName}``` -- the stemmed inverted index for a particular field.
* ```extent``` -- the start and end positions of each field in the documents.
* ```lengths``` -- document lengths.
* ```names``` and ```names.reverse``` -- the mapping between internal and external IDs (docnos).
* ```corpus/``` -- stores the content of the indexed documents.

You need to use different classes to operate different index files (see following examples in the tutorial).

#### Lucene

Lucene uses the IndexReader class to operate all index files.

```java
// modify to your index path
String pathIndex = "index_example_lucene"; 

// First, open the directory
Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

// Then, open an IndexReader to access your index
IndexReader index = DirectoryReader.open( dir );

// Now, start working with your index using the IndexReader object

ixreader.numDocs(); // just an example; get the number of documents in the index

// Remember to close both the IndexReader and the Directory after use 
index.close();
dir.close();
``` 

### External and Internal IDs

In IR experiments, we often use some unique IDs to identify documents in the corpus. 
For example, our example corpus (and most TREC corpora) uses docno as the unique identifer.

However, IR systems often use some internal IDs to identify the indexed documents.
These IDs are often subject to change and not transparent to the users.
So you often need to transform between external and internal IDs when locating documents in an index.

#### Galago

Many different objects in Galago provide the mapping between internal and external IDs.
For example, you can use ```DiskIndex```:
```java
DiskIndex index = new DiskIndex( indexPath );

long docid = 5;
index.getName( docid ); // get the docno for the internal docid = 5

String docno = "ACM-1835461";
index.getIdentifier( docno ); // get the internal docid for docno "ACM-1835461"
```

You can also use ```Retrieval```:
```java
Retrieval retrieval = RetrievalFactory.instance( indexPath, Parameters.create() );

long docid = 5;
retrieval.getDocumentName( (int) docid ); // get the docno for the internal docid = 5
// Well, Galago has some inconsistencies regarding whether to use integer or long integer for internal docids.

String docno = "ACM-1835461";
retrieval.getDocumentId( docno ); // get the internal docid for docno "ACM-1835461"
```

#### Lucene

Transforming between external and internal IDs is a little bit more complex in Lucene 
(because it is not designed for IR research). To help you get started quickly, we provide
a utility class ```edu.cs.umass.cs646.utils.LuceneUtils``` to help you transform between 
the two IDs.

Note that unlike Galago (which automatically parses and stores the docno field as metadata), 
you need to appropriately index and store the docno field by yourself in Lucene (read the 
part of the tutorial about building a Lucene index). 
```java
IndexReader index = DirectoryReader.open( dir );

// the name of the field storing external IDs (docnos)
String fieldName = "docno";

int docid = 5;
LuceneUtils.getDocno( index, fieldName, docid ); // get the docno for the internal docid = 5

String docno = "ACM-1835461";
LuceneUtils.findByDocno( index, fieldName, docno ); // get the internal docid for docno "ACM-1835461"
```

### Frequency Posting List

You can retrieve a term's posting list from an index.
The simplest form is document-frequency posting list,
where each entry in the list is a ```<docid,frequency>``` pair (only includes the documents containing that term).
The entries are sorted by docids such that you can efficiently compare and merge multiple lists.

#### Galago

```java
String pathIndex = "/home/jiepu/Downloads/example_index_galago/";

// Let's just retrieve the posting list for the term "reformulation" in the "text" field
String field = "text";
String term = "retrieval";

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
```

The output is:
```
DOCID     DOCNO          FREQ      
3         ACM-2010085    1         
10        ACM-1835626    2         
24        ACM-1277796    1         
94        ACM-2484096    1         
98        ACM-2348355    4         
104       ACM-2609633    1         
```

#### Lucene

```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
			
// Let's just retrieve the posting list for the term "reformulation" in the "text" field
String field = "text";
String term = "reformulation";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

// The following line reads the posting list of the term in a specific index field.
// You need to encode the term into a BytesRef object,
// which is the internal representation of a term used by Lucene.
System.out.printf( "%-10s%-15s%-6s\n", "DOCID", "DOCNO", "FREQ" );
PostingsEnum posting = MultiFields.getTermDocsEnum( index, field, new BytesRef( term ), PostingsEnum.FREQS );
if ( posting != null ) { // if the term does not appear in any document, the posting object may be null
    int docid;
    // Each time you call posting.nextDoc(), it moves the cursor of the posting list to the next position
    // and returns the docid of the current entry (document). Note that this is an internal Lucene docid.
    // It returns PostingsEnum.NO_MORE_DOCS if you have reached the end of the posting list.
    while ( ( docid = posting.nextDoc() ) != PostingsEnum.NO_MORE_DOCS ) {
        String docno = LuceneUtils.getDocno( index, "docno", docid );
        int freq = posting.freq(); // get the frequency of the term in the current document
        System.out.printf( "%-10d%-15s%-6d\n", docid, docno, freq );
    }
}

index.close();
dir.close();
```

The output is:
```
DOCID     DOCNO          FREQ  
3         ACM-2010085    1     
10        ACM-1835626    2     
24        ACM-1277796    1     
94        ACM-2484096    1     
98        ACM-2348355    4     
104       ACM-2609633    1     
```

Note that the internal docids are subject to change and 
are often different between different systems. So it is 
only a coincidence that the docids from the two systems
are the same.

### Position Posting List

You can also retrieve a posting list with term postions in each document.

#### Galago

```java
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

System.out.printf( "%-10s%-15s%-10s%-20s\n", "DOCID", "DOCNO", "FREQ", "POSITIONS" );

KeyIterator vocabulary = posting.getIterator();
// try to locate the term in the vocabulary
if ( vocabulary.skipToKey( ByteUtil.fromString( term ) ) && term.equals( vocabulary.getKeyString() ) ) {
    // get an iterator for the term's posting list
    ExtentIterator iterator = (ExtentIterator) vocabulary.getValueIterator();
    ScoringContext sc = new ScoringContext();
    while ( !iterator.isDone() ) {
        
        // Get the current entry's document id.
        // Note that you need to assign the value of sc.document,
        // otherwise count(sc) and others will not work correctly.
        sc.document = iterator.currentCandidate();
        int freq = iterator.count( sc ); // get the frequency of the
        String docno = index.getName( sc.document ); // get the external ID
        
        // Get the occurrence positions of the term in the current document
        ExtentArray extents = iterator.extents( sc );
        System.out.printf( "%-10s%-15s%-10s", sc.document, docno, freq );
        for ( int i = 0; i < extents.size(); i++ ) {
            System.out.print( ( i > 0 ? "," : "" ) + extents.begin( i ) );
            // Note that extents.end( i ) gives you the end position of an "expression" (such as a term,
            // an ordered phrase, or an unordered phrase in Galago). In case of a single term,
            // extents.end( i ) = extents.begin( i ) + 1.
        }
        System.out.println();
        // Jump to the entry right after the current one.
        iterator.movePast( iterator.currentCandidate() );
    }
}

posting.close();
index.close();
```

The output is:
```
DOCID     DOCNO          FREQ      POSITIONS           
3         ACM-2010085    1         82
10        ACM-1835626    2         28,100
24        ACM-1277796    1         185
94        ACM-2484096    1         41
98        ACM-2348355    4         110,143,182,203
104       ACM-2609633    1         190
```

#### Lucene

```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";

// Let's just retrieve the posting list for the term "reformulation" in the "text" field
String field = "text";
String term = "reformulation";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

// we also print out external ID
Set<String> fieldset = new HashSet<>();
fieldset.add( "docno" );

// The following line reads the posting list of the term in a specific index field.
// You need to encode the term into a BytesRef object,
// which is the internal representation of a term used by Lucene.
System.out.printf( "%-10s%-15s%-10s%-20s\n", "DOCID", "DOCNO", "FREQ", "POSITIONS" );
PostingsEnum posting = MultiFields.getTermDocsEnum( index, field, new BytesRef( term ), PostingsEnum.POSITIONS );
if ( posting != null ) { // if the term does not appear in any document, the posting object may be null
    int docid;
    // Each time you call posting.nextDoc(), it moves the cursor of the posting list to the next position
    // and returns the docid of the current entry (document). Note that this is an internal Lucene docid.
    // It returns PostingsEnum.NO_MORE_DOCS if you have reached the end of the posting list.
    while ( ( docid = posting.nextDoc() ) != PostingsEnum.NO_MORE_DOCS ) {
        String docno = index.document( docid, fieldset ).get( "docno" );
        int freq = posting.freq(); // get the frequency of the term in the current document
        System.out.printf( "%-10d%-15s%-10d", docid, docno, freq );
        for ( int i = 0; i < freq; i++ ) {
            // Get the next occurrence position of the term in the current document.
            // Note that you need to make sure by yourself that you at most call this function freq() times.
            System.out.print( ( i > 0 ? "," : "" ) + posting.nextPosition() );
        }
        System.out.println();
    }
}

index.close();
dir.close();
```

The output is:
```
DOCID     DOCNO          FREQ      POSITIONS           
3         ACM-2010085    1         56
10        ACM-1835626    2         1,73
24        ACM-1277796    1         157
94        ACM-2484096    1         12
98        ACM-2348355    4         84,117,156,177
104       ACM-2609633    1         153
```

You probably have already noticed that the positions retrieved by
Galago and Lucene are different. This is because the positions by 
Galago are relative to the start of the whole document (even when 
we are retrieving a particular field's posting list), while the 
Lucene ones are relative to the start of the field.

### Accessing An Indexed Document

You can access an indexed document from an index.

#### Galago

```java
String pathIndexBase = "/home/jiepu/Downloads/example_index_galago/";

// Let's just retrieve the document vector (only the "text" field) for the doc with docid=21 from a Galago index.
String field = "text";
int docid = 21;

// make sure you stored the corpus when building index (--corpus=true)
Retrieval index = RetrievalFactory.instance( pathIndexBase );
Document.DocumentComponents dc = new Document.DocumentComponents( false, false, true );

// now you've retrieved a document stored in the index (including all fields)
Document doc = index.getDocument( index.getDocumentName( docid ), dc );

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
System.out.printf( "%-20s%-10s%-20s\n", "TERM", "FREQ", "POSITIONS" );
for ( String term : docVector.keySet() ) {
    System.out.printf( "%-20s%-10s", term, docVector.get( term ).size() );
    List<Integer> positions = docVector.get( term );
    for ( int i = 0; i < positions.size(); i++ ) {
        System.out.print( ( i > 0 ? "," : "" ) + positions.get( i ) );
    }
    System.out.println();
}
```

The output is:
```
TERM                FREQ      POSITIONS           
1                   1         124
2007                1         111
800                 1         125
95\                 1         181
a                   2         51,152
acquire             1         118
algorithms          1         116
along               1         133
analysis            1         136
and                 3         74,144,165
appreciable         1         184
are                 1         83
as                  2         166,168
assessor            1         175
at                  1         109
available           1         84
be                  2         91,178
been                2         37,50
best                1         68
between             1         139
by                  1         180
can                 1         177
completeness        1         47
cost                1         163
deal                1         53
deeper              1         135
document            1         114
documents           1         71
dozen               1         41
each                1         43
effective           1         164
effort              2         104,176
errors              1         188
estimate            1         77
evaluate            1         94
evaluation          5         34,58,78,154,187
few                 1         81
fewer               2         159,169
for                 1         121
great               1         52
has                 2         35,49
how                 2         64,75
in                  2         85,186
increase            1         185
information         1         32
investigating       1         137
is                  1         161
it                  1         89
judge               1         73
judged              1         44
judging             1         103
judgment            1         62
judgments           5         82,120,147,160,173
light               1         86
many                1         96
measures            1         79
million             1         106
more                6         97,101,122,156,162,172
much                2         60,100
near                1         46
no                  1         183
number              2         141,145
of                  6         54,70,87,130,142,146
on                  1         57
over                4         39,59,95,155
performed           1         38
point               1         153
possible            1         92
present             1         128
queries             6         42,98,126,143,157,170
query               1         107
recent              1         55
reduced             1         179
relevance           1         119
reliable            1         167
results             1         129
retrieval           1         33
select              1         66
selection           1         115
set                 1         69
sets                1         63
several             1         40
should              1         90
shows               1         148
smaller             1         61
than                1         123
that                1         149
the                 4         67,105,131,140
there               1         48
this                1         88
to                  7         45,65,72,76,93,117,151
total               2         102,174
track               2         108,132
tradeoffs           1         138
trec                1         110
two                 1         113
typically           1         36
up                  1         150
used                1         112
we                  1         127
when                1         80
with                4         134,158,171,182
without             1         99
work                1         56
```

#### Lucene

```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";

// let's just retrieve the document vector (only the "text" field) for the Document with internal ID=21
String field = "text";
int docid = 21;

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

Terms vector = index.getTermVector( docid, field ); // Read the document's document vector.

// You need to use TermsEnum to iterate each entry of the document vector (in alphabetical order).
System.out.printf( "%-20s%-10s%-20s\n", "TERM", "FREQ", "POSITIONS" );
TermsEnum terms = vector.iterator();
PostingsEnum positions = null;
BytesRef term;
while ( ( term = terms.next() ) != null ) {
    
    String termstr = term.utf8ToString(); // Get the text string of the term.
    long freq = terms.totalTermFreq(); // Get the frequency of the term in the document.
    
    System.out.printf( "%-20s%-10d", termstr, freq );
    
    // Lucene's document vector can also provide the position of the terms
    // (in case you stored these information in the index).
    // Here you are getting a PostingsEnum that includes only one document entry, i.e., the current document.
    positions = terms.postings( positions, PostingsEnum.POSITIONS );
    positions.nextDoc(); // you still need to move the cursor
    // now accessing the occurrence position of the terms by iteratively calling nextPosition()
    for ( int i = 0; i < freq; i++ ) {
        System.out.print( ( i > 0 ? "," : "" ) + positions.nextPosition() );
    }
    System.out.println();
    
}

index.close();
dir.close();
```

```
TERM                FREQ      POSITIONS           
1,800               1         92
2007                1         79
95                  1         148
a                   2         19,119
acquire             1         86
algorithms          1         84
along               1         100
analysis            1         103
and                 3         42,111,132
appreciable         1         151
are                 1         51
as                  2         133,135
assessor            1         142
at                  1         77
available           1         52
be                  2         59,145
been                2         5,18
best                1         36
between             1         106
by                  1         147
can                 1         144
completeness        1         15
cost                1         130
deal                1         21
deeper              1         102
document            1         82
documents           1         39
dozen               1         9
each                1         11
effective           1         131
effort              2         72,143
errors              1         155
estimate            1         45
evaluate            1         62
evaluation          5         2,26,46,121,154
few                 1         49
fewer               2         126,136
for                 1         89
great               1         20
has                 2         3,17
how                 2         32,43
in                  2         53,153
increase            1         152
information         1         0
investigating       1         104
is                  1         128
it                  1         57
judge               1         41
judged              1         12
judging             1         71
judgment            1         30
judgments           5         50,88,114,127,140
light               1         54
many                1         64
measures            1         47
million             1         74
more                6         65,69,90,123,129,139
much                2         28,68
near                1         14
no                  1         150
number              2         108,112
of                  6         22,38,55,97,109,113
on                  1         25
over                4         7,27,63,122
performed           1         6
point               1         120
possible            1         60
present             1         95
queries             6         10,66,93,110,124,137
query               1         75
recent              1         23
reduced             1         146
relevance           1         87
reliable            1         134
results             1         96
retrieval           1         1
select              1         34
selection           1         83
set                 1         37
sets                1         31
several             1         8
should              1         58
shows               1         115
smaller             1         29
than                1         91
that                1         116
the                 4         35,73,98,107
there               1         16
this                1         56
to                  7         13,33,40,44,61,85,118
total               2         70,141
track               2         76,99
tradeoffs           1         105
trec                1         78
two                 1         81
typically           1         4
up                  1         117
used                1         80
we                  1         94
when                1         48
with                4         101,125,138,149
without             1         67
work                1         24
```

There are some slight differences between the results by Lucene and Galago.
They are caused by the differences of the two systems in default tokenization. 
For example, Lucene will not split 1,800 into two tokens but Galado will.

### Document and Field Length

#### Galago

```DiskIndex``` provides access to the length of the whole document:
```java
DiskIndex index = new DiskIndex( pathIndex );

long docid = 5;
index.getLength( docid );
```

Getting the length of a particular document field is a little bit more complex:

The following program iteratively prints out the length of the text field for each document in the index:
```java
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
```

The output is:
```
DOCID     DOCNO          Length    
0         ACM-2009969    187       
1         ACM-2009998    151       
2         ACM-2010026    136       
3         ACM-2010085    117       
4         ACM-1835458    142       
5         ACM-1835461    132       
6         ACM-1835493    175       
7         ACM-1835499    171       
8         ACM-1835521    156       
9         ACM-1835602    92        
10        ACM-1835626    112       
11        ACM-1835637    67        
12        ACM-1835650    110       
13        ACM-1572050    130       
14        ACM-1572139    99        
15        ACM-1572140    83        
16        ACM-1390339    187       
17        ACM-1390376    165       
18        ACM-1390416    117       
19        ACM-1390419    134       
20        ACM-1390432    110       
21        ACM-1390445    157       
22        ACM-1277758    168       
23        ACM-1277774    91        
24        ACM-1277796    158       
25        ACM-1277835    170       
26        ACM-1277868    69        
27        ACM-1277920    59        
28        ACM-1277922    155       
29        ACM-1277947    107       
30        ACM-1148204    140       
31        ACM-1148212    99        
32        ACM-1148219    246       
33        ACM-1148250    136       
34        ACM-1148305    102       
35        ACM-1148310    5         
36        ACM-1148324    104       
37        ACM-1076074    164       
38        ACM-1076109    167       
39        ACM-1076115    122       
40        ACM-1076156    100       
41        ACM-1076190    5         
42        ACM-1009026    96        
43        ACM-1009044    102       
44        ACM-1009098    86        
45        ACM-1009110    5         
46        ACM-1009114    5         
47        ACM-1008996    149       
48        ACM-860437     1253      
49        ACM-860479     168       
50        ACM-860493     107       
51        ACM-860548     5         
52        ACM-860549     37        
53        ACM-564394     95        
54        ACM-564408     152       
55        ACM-564429     151       
56        ACM-564430     78        
57        ACM-564441     5         
58        ACM-564465     58        
59        ACM-383954     105       
60        ACM-383972     115       
61        ACM-384022     121       
62        ACM-345674     5         
63        ACM-345546     137       
64        ACM-312679     5         
65        ACM-312687     5         
66        ACM-312698     5         
67        ACM-290954     5         
68        ACM-290958     5         
69        ACM-290987     5         
70        ACM-291008     5         
71        ACM-291043     5         
72        ACM-258540     5         
73        ACM-258547     5         
74        ACM-243202     5         
75        ACM-243274     5         
76        ACM-243276     5         
77        ACM-215328     5         
78        ACM-215380     5         
79        ACM-188586     5         
80        ACM-160728     85        
81        ACM-160760     5         
82        ACM-160761     76        
83        ACM-160689     81        
84        ACM-133203     145       
85        ACM-122864     5         
86        ACM-636811     81        
87        ACM-511797     5         
88        ACM-636717     93        
89        ACM-511760     197       
90        ACM-511717     124       
91        ACM-803136     129       
92        ACM-2484097    206       
93        ACM-2484069    249       
94        ACM-2484096    191       
95        ACM-2484060    279       
96        ACM-2484139    157       
97        ACM-2348296    157       
98        ACM-2348355    189       
99        ACM-2348408    157       
100       ACM-2348426    80        
101       ACM-2348440    5         
102       ACM-2609628    170       
103       ACM-2609467    136       
104       ACM-2609633    245       
105       ACM-2609503    126       
106       ACM-2609485    109       
107       ACM-2609536    194
```

#### Lucene

Unfortunately, by the time I created the tutorial, Lucene does not store document length in its index. 
An acceptable but slow solution is that you calculate document length by yourself based on a document
vector. In case your dataset is static and relatively small (such as just about or less than a few
million documents), you can simply compute all documents' lengths after you've built an index and store
them in an external file (it takes just 4MB to store 1 million docs' lengths as integers). At running
time, you can load all the computed document lengths into memory to avoid loading doc vector and computing length again.

The following program prints out the length of text field for each document in the example corpus:
```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
String field = "text";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader ixreader = DirectoryReader.open( dir );

// we also print out external ID
Set<String> fieldset = new HashSet<>();
fieldset.add( "docno" );

// The following loop iteratively print the lengths of the documents in the index.
System.out.printf( "%-10s%-15s%-10s\n", "DOCID", "DOCNO", "Length" );
for ( int docid = 0; docid < ixreader.maxDoc(); docid++ ) {
    String docno = ixreader.document( docid, fieldset ).get( "docno" );
    int doclen = 0;
    // Unfortunately, Lucene does not store document length in its index
    // (because its retrieval model does not rely on document length).
    // An acceptable but slow solution is that you calculate document length by yourself based on
    // document vector. In case your dataset is static and relatively small (such as about or less
    // than a few million documents), you can simply compute the document lengths and store them in
    // an external file (it takes just a few MB). At running time, you can load all the computed
    // document lengths to avoid loading doc vector and computing length.
    TermsEnum termsEnum = ixreader.getTermVector( docid, field ).iterator();
    while ( termsEnum.next() != null ) {
        doclen += termsEnum.totalTermFreq();
    }
    System.out.printf( "%-10d%-15s%-10d\n", docid, docno, doclen );
}

ixreader.close();
dir.close();
```

The output is:
```
DOCID     DOCNO          Length    
0         ACM-2009969    187       
1         ACM-2009998    151       
2         ACM-2010026    136       
3         ACM-2010085    117       
4         ACM-1835458    142       
5         ACM-1835461    132       
6         ACM-1835493    175       
7         ACM-1835499    171       
8         ACM-1835521    156       
9         ACM-1835602    92        
10        ACM-1835626    112       
11        ACM-1835637    67        
12        ACM-1835650    110       
13        ACM-1572050    130       
14        ACM-1572139    99        
15        ACM-1572140    83        
16        ACM-1390339    187       
17        ACM-1390376    165       
18        ACM-1390416    117       
19        ACM-1390419    134       
20        ACM-1390432    110       
21        ACM-1390445    156       
22        ACM-1277758    168       
23        ACM-1277774    91        
24        ACM-1277796    158       
25        ACM-1277835    170       
26        ACM-1277868    69        
27        ACM-1277920    59        
28        ACM-1277922    155       
29        ACM-1277947    107       
30        ACM-1148204    140       
31        ACM-1148212    99        
32        ACM-1148219    246       
33        ACM-1148250    136       
34        ACM-1148305    102       
35        ACM-1148310    5         
36        ACM-1148324    104       
37        ACM-1076074    159       
38        ACM-1076109    167       
39        ACM-1076115    122       
40        ACM-1076156    100       
41        ACM-1076190    5         
42        ACM-1009026    96        
43        ACM-1009044    102       
44        ACM-1009098    86        
45        ACM-1009110    5         
46        ACM-1009114    5         
47        ACM-1008996    149       
48        ACM-860437     1253      
49        ACM-860479     167       
50        ACM-860493     107       
51        ACM-860548     5         
52        ACM-860549     37        
53        ACM-564394     95        
54        ACM-564408     152       
55        ACM-564429     151       
56        ACM-564430     78        
57        ACM-564441     5         
58        ACM-564465     58        
59        ACM-383954     105       
60        ACM-383972     115       
61        ACM-384022     120       
62        ACM-345674     5         
63        ACM-345546     137       
64        ACM-312679     5         
65        ACM-312687     5         
66        ACM-312698     5         
67        ACM-290954     5         
68        ACM-290958     5         
69        ACM-290987     5         
70        ACM-291008     5         
71        ACM-291043     5         
72        ACM-258540     5         
73        ACM-258547     5         
74        ACM-243202     5         
75        ACM-243274     5         
76        ACM-243276     5         
77        ACM-215328     5         
78        ACM-215380     5         
79        ACM-188586     5         
80        ACM-160728     85        
81        ACM-160760     5         
82        ACM-160761     76        
83        ACM-160689     81        
84        ACM-133203     145       
85        ACM-122864     5         
86        ACM-636811     81        
87        ACM-511797     5         
88        ACM-636717     93        
89        ACM-511760     195       
90        ACM-511717     124       
91        ACM-803136     129       
92        ACM-2484097    206       
93        ACM-2484069    249       
94        ACM-2484096    191       
95        ACM-2484060    278       
96        ACM-2484139    157       
97        ACM-2348296    157       
98        ACM-2348355    189       
99        ACM-2348408    157       
100       ACM-2348426    80        
101       ACM-2348440    5         
102       ACM-2609628    170       
103       ACM-2609467    136       
104       ACM-2609633    245       
105       ACM-2609503    126       
106       ACM-2609485    109       
107       ACM-2609536    194       
```

### Corpus-level Statistics

#### Galago

```Retrieval``` provides many corpus-level statistics.
The follow program computes the IDF and corpus probability for the term ```reformulation```.
```java
String pathIndexBase = "/home/jiepu/Downloads/example_index_galago/";

// Let's just count the IDF and P(w|corpus) for the word "reformulation" in the "text" field
String field = "text";
String term = "reformulation";

Retrieval retrieval = RetrievalFactory.instance( pathIndexBase );

Node termNode = StructuredQuery.parse( "#text:" + term + ":part=field." + field + "()" );
termNode.getNodeParameters().set( "queryType", "count" );

NodeStatistics termStats = retrieval.getNodeStatistics( termNode );
long corpusTF = termStats.nodeFrequency; // Get the total frequency of the term in the text field
long n = termStats.nodeDocumentCount; // Get the document frequency (DF) of the term (only counting the text field)

Node fieldNode = StructuredQuery.parse( "#lengths:" + field + ":part=lengths()" );
FieldStatistics fieldStats = retrieval.getCollectionStatistics( fieldNode );
long corpusLength = fieldStats.collectionLength; // Get the length of the corpus (only counting the text field)
long N = fieldStats.documentCount; // Get the total number of documents

double idf = Math.log( ( N + 1 ) / ( n + 1 ) ); // well, we normalize N and n by adding 1 to avoid n = 0
double pwc = 1.0 * corpusTF / corpusLength;

System.out.printf( "%-30sN=%-10dn=%-10dIDF=%-8.2f\n", term, N, n, idf );
System.out.printf( "%-30slen(corpus)=%-10dfreq(%s)=%-10dP(%s|corpus)=%-10.6f\n", term, corpusLength, term, corpusTF, term, pwc );

retrieval.close();
```

The output is:
```
reformulation                 N=108       n=6         IDF=2.71    
reformulation                 len(corpus)=12088     freq(reformulation)=10        P(reformulation|corpus)=0.000827  
```

#### Lucene

```IndexReader``` provides many corpus-level statistics.
The follow program computes the IDF and corpus probability for the term ```reformulation```.
```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
			
// Let's just count the IDF and P(w|corpus) for the word "reformulation" in the "text" field
String field = "text";
String term = "reformulation";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

int N = index.numDocs(); // the total number of documents in the index
int n = index.docFreq( new Term( field, term ) ); // get the document frequency of the term in the "text" field
double idf = Math.log( ( N + 1 ) / ( n + 1 ) ); // well, we normalize N and n by adding 1 to avoid n = 0

System.out.printf( "%-30sN=%-10dn=%-10dIDF=%-8.2f\n", term, N, n, idf );

long corpusTF = index.totalTermFreq( new Term( field, term ) ); // get the total frequency of the term in the "text" field
long corpusLength = index.getSumTotalTermFreq( field ); // get the total length of the "text" field
double pwc = 1.0 * corpusTF / corpusLength;

System.out.printf( "%-30slen(corpus)=%-10dfreq(%s)=%-10dP(%s|corpus)=%-10.6f\n", term, corpusLength, term, corpusTF, term, pwc );

// remember to close the index and the directory
index.close();
dir.close();
```

The output is:
```
reformulation                 N=108       n=6         IDF=2.71    
reformulation                 len(corpus)=12077     freq(reformulation)=10        P(reformulation|corpus)=0.000828
```

## Searching

Coming soon ... 
