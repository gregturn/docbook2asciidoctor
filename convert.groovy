import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*

class C {
}

@Log
class Section {
    def qName, attrs, level, chunks=[]
    
    String render() {
        def results = ""
        if (attrs['id'] != null) {
            results += "[[${attrs['id']}]]\n"
        } else if (attrs['xml:id'] != null) {
            results += "[[${attrs['xml:id']}]]\n"
        }
        chunks.each { chunk ->
            if (chunk.metaClass.respondsTo(chunk, "render")) {
                results += chunk.render()
            } else {
                log.info("Not sure how to handle ${chunk}")
            }
        }
        results
    }
    
    def stripped() {
        chunks.collect { chunk ->
            if (chunk.metaClass.respondsTo(chunk, "render")) {
                chunk.render()
            } else {
                chunk.replaceAll('\\s+', ' ')
            }
        }.join('').trim()    
    }
    
    
    String toString() {
        "Section qName:${qName} level:${level}, chunks:${chunks} attrs:${attrs}"
    }
}

class Example {
    def section
    
    String render() {
        def title = section.chunks.find { it.section.qName == "title"}
        if (title != null) {
            title.section.attrs['context'] = 'example'
        }    
        def results = "===="
        results += section.chunks.collect { chunk ->
            chunk.render()
        }.join('')
        results += "====\n\n"
    }
    
    String toString() { "Example ${section}"}
}

class Title {
    def section, level
    
    String render() {
        if (["table", "example"].contains(section.attrs['context'])) {
            "\n\n.${section.stripped()}\n"
        } else {
            def results = "${'='*level} ${section.stripped()}"
            if (section.attrs['subtitle'] != null) {
                results += "\n\n*_${section.attrs['subtitle'].stripped()}_*"
            }
            results += "\n\n"
            results
        }
    }
    
    String toString() {
        "Title: ${'='*level} ${section.chunks}"
    }
}

class ProgramListing {
    def section
    
    String render() {
        def results = ""
        if (section.attrs['lang'] != null) {
            results += "[source,${section.attrs['lang']}]\n"
        } else if (section.attrs['language'] != null) {
            results += "[source,${section.attrs['language']}]\n"
        } else {
            results += "[source]\n"
        }
        results += "----\n"
        //results += "${section.chunks.join('').trim()}\n"
        results += section.chunks.collect { chunk ->
            "${chunk}"
        }.join('')
        results += "\n----\n\n"
        results
    }
    
    String toString() {
        "ProgramListing: chunks:${section.chunks} lang:${section.attrs['lang']}"
    }
}

class Paragraph {
    def section
    
    String render() {
        "${section.stripped()}\n\n"
    }
    
    String toString() {
        "Paragraph: chunks:${section.chunks}"
    }
}

class Monospaced {
    def section
    
    String render() {
        "`${section.chunks.join('').trim()}`"
    }
    
    String toString() {
        "Monospaced (${section.chunks})"
    }
}

class Ulink {
    def section
    
    String render() {
        if (section.chunks.size() > 0) {
            "${section.attrs['url']}[${section.stripped()}]"
        }
    }
    
    String toString() { "Ulink (${section})"}
}

class Xref {
    def section
    
    String render() {
        if (section.chunks.size() > 0) {
            "<<${section.attrs['linked']},${section.stripped()}>>"
        } else {
            "<<${section.attrs['linkend']}>>"
        }
    }
    
    String toString() { "Xref (${section})"}
}

class Emphasis {
    def section
    
    String render() {
        "*${section.chunks.join('')}*"
    }
    
    String toString() { "Bold ${section}"}
}

class Note {
    def section
    
    String render() {
        "NOTE: ${section.stripped()}\n\n"
    }
    
    String toString() { "Note ${section}"}
}

class Important {
    def section

    String render() {
        "IMPORTANT: ${section.stripped()}\n\n"
    }

    String toString() { "IMPORTANT ${section}"}
}

class ImageData {
    def section
    
    String render() { 
        "image::${section.attrs['fileref']}[]\n\n"
    }
    
    String toString() { "ImageData ${section}"}
}

class ImageObject {
    def section
    
    String render() { 
        "${section.chunks[0].render()}"
    }

    String toString() { "ImageObject ${section}"}
}

class MediaObject {
    def section
    
    String render() { 
        "${section.chunks[0].render()}"
    }
    
    String toString() { "MediaObject ${section}"}
}

class Screenshot {
    def section
    
    String render() { 
        "${section.chunks[0].render()}"
    }
    
    String toString() { "Screenshot ${section}"}
}

class Term {
    def section
    
    String render() { 
        "${section.stripped()}::"
    }
    
    String toString() { "Term ${section}"}
}

class ListItem {
    def section
    
    String render() { 
        "${section.stripped()}"
    }
    
    String toString() { "ListItem ${section}"}
}

class VarListEntry {
    def section
    
    String render() { 
        "${section.chunks[0].render()}\n${section.chunks[1].render()}\n"
    }
    
    String toString() { "VarListEntry ${section}"}
}

class VariableList {
    def section
    
    String render() { 
        "\n\n" + section.chunks.collect { chunk ->
            chunk.render()
        }.join("\n")
    }
    
    String toString() { "VariableList ${section}"}
}

class OrderedList {
    def section
    
    String render() { 
        section.chunks.collect { chunk ->
            ". ${chunk.render()}"
        }.join("\n") + "\n\n"
    }
    
    String toString() { "OrderedList ${section}"}
}

class ItemizedList {
    def section
    
    String render() {
        section.chunks.collect { chunk ->
            "* ${chunk.render()}"
        }.join("\n") + "\n\n"
    }
    
    String toString() { "ItemizedList ${section}"}
}

class Include {
    def section
    def spring_data_commons_path = "https://raw.github.com/SpringSource/spring-data-commons/1.9.0.M1/src/docbkx"
    
    String render() { 
        if (section.attrs['href'].contains(spring_data_commons_path)) {
            def results = "include::{spring-data-commons-docs}/${section.attrs['href']-spring_data_commons_path-'xml'+'adoc'}[]\n"
            results += "// Put the following line at the top...\n"
            results += ":spring-data-commons-docs: https://raw.githubusercontent.com/spring-projects/spring-data-commons/master/src/main/asciidoc"
            results
        } else {
            "include::${section.attrs['href']-'xml'+'adoc'}[]\n"
        }
    }
    
    String toString() { "Include ${section}"}
}

class Firstname {
    def section
    
    String render() { "+++ ${toString()}"}
    
    String toString() { "Firstname ${section}"}
}

class Surname {
    def section
    
    String render() { "+++ ${toString()}"}
    
    String toString() { "Surname ${section}"}
}

class Author {
    def section
    
    String render() { "+++ ${toString()}"}
    
    String toString() { "Author ${section}"}
}

class AuthorGroup {
    def section
    
    String render() { "+++ ${toString()}"}
    
    String toString() { "AuthorGroup ${section}"}
}

class PlainText {
    def section

    String render() { "${section.chunks[0]}" }

    String toString() { "PlainText ${section}" }
}

class Quote {
    def section

    String render() { "[quote] \n${section.stripped()}\n\n" }

    String toString() { "PlainText ${section}" }
}

class Table {
    def section

    String render() { 
        def title = section.chunks.find { it.section.qName == "title"}
        if (title != null) {
            title.section.attrs['context'] = 'table'
        }
        section.chunks.collect { chunk ->
            chunk.render()
        }.join('')
    }

    String toString() { "Table ${section}" }
}

class TableGroup {
    def section

    String render() { 
        def results = "|===\n"

        if (section.chunks.any{it.section.qName == 'colspec'} || 
            section.chunks.any{it.section.qName == 'thead'}) {
            
            def tableDef = "["
            tableDef += 'cols="' + section.chunks
                .findAll{it.section.qName == 'colspec'}.collect {
                    it.section.attrs['colwidth']-'*'
                }.join(',') + '"'
            
            if (section.chunks.any{it.section.qName == 'thead'}) {
                tableDef += ', options="header"]\n'
            } else {
                tableDef += "]\n"
            }
            tableDef += "// Move this line above the title\n"
            results += tableDef
            
        }
                
        results += section.chunks.collect { chunk ->
            chunk.render()
        }.join('')
        results += "|===\n"
        results
    }

    String toString() { "TableGroup ${section}" }
}

class ColumnSpec {
    def section

    String render() { "" }

    String toString() { "ColumnSpec ${section}" }
}

class TableHead {
    def section

    String render() { 
        section.chunks.collect { chunk ->
            chunk.render()
        }.join('')
    }

    String toString() { "TableHead ${section}" }
}

class Row {
    def section

    String render() { 
        section.chunks.collect { chunk ->
            chunk.render()
        }.join('\n')
    }

    String toString() { "Row ${section}" }
}

class Entry {
    def section

    String render() { "| ${section.stripped()}" }

    String toString() { "Entry ${section}" }
}

class TableBody {
    def section

    String render() { 
        section.chunks.collect { chunk ->
            chunk.render()
        }.join('\n\n') + "\n"
    }

    String toString() { "TableBody ${section}" }
}

class Bridgehead {
    def section
    
    String render() {
        "[float]\n${'='*section.level} ${section.stripped()}\n\n"
    }
    
    String toString() { "Bridgehead ${section}"}
}


@Log
class Docbook5Handler extends DefaultHandler {

    File doc    
    def sectionStack = []
    def asciidoc = ""
    def rootSection
    
    Docbook5Handler() {
    }
    
    Docbook5Handler(File doc) {
        this.doc = doc
    }
    
    def extractAllAttrs(Attributes attrs) {
        def extractedAttrs = [:]
        for (int i=0; i < attrs.length; i++) {
            extractedAttrs[attrs.getQName(i)] = attrs.getValue(i)
        }
        extractedAttrs
    }
    
    void startElement(String ns, String localName, String qName, Attributes attrs) {
        def extractedAttrs = extractAllAttrs(attrs)
        log.info("startElement: ${qName} has attrs ${extractedAttrs}")
        if (sectionStack.size() == 0) {
            sectionStack.push(new Section([qName:qName, attrs:extractedAttrs, level:1]))
            log.info("PUSH ${qName}: Creating level 1 section")
        } else {
            sectionStack.push(new Section([qName:qName, attrs:extractedAttrs, 
                level:sectionStack[-1].level+1]))
            log.info("PUSH ${qName}: Creating level ${sectionStack[-1].level} section")
        }
    }
    
    void characters(char[] chars, int offset, int length) {
        sectionStack[-1].chunks += new String(chars, offset, length)
    }
    
    void endElement(String ns, String localName, String qName) {
        def section = sectionStack.pop()
        log.info("POP ${qName}: ${section}")
        if (sectionStack.size() == 0) {
            rootSection = section
        } else {
            if (qName == "title") {
                sectionStack[-1].chunks += new Title([section:section, level:sectionStack[-1].level])
            } else if (qName == "subtitle") {
                // Find existing title and add attrs['subtitle']
                def title = sectionStack[-1].chunks.find{it.section.qName == 'title'}
                title.section.attrs['subtitle'] = section
                log.info("${title} updated with subtitle")
            } else if (qName == "programlisting") {
                sectionStack[-1].chunks += new ProgramListing([section:section])
            } else if (qName == "para") {
                sectionStack[-1].chunks += new Paragraph([section:section])
            } else if (["classname", "code", "literal", "interfacename","methodname"].contains(qName)) {
                sectionStack[-1].chunks += new Monospaced([section:section])
            } else if (["section", "part", "partintro", "simpara", "abstract", "simplesect"].contains(qName)) {
                sectionStack[-1].chunks += section
            } else if (qName == "example") {
                sectionStack[-1].chunks += new Example([section:section])
            } else if (qName == "ulink") {
                sectionStack[-1].chunks += new Ulink([section:section])
            } else if (["xref", "link"].contains(qName)) {
                sectionStack[-1].chunks += new Xref([section:section])
            } else if (qName == "emphasis") {
                sectionStack[-1].chunks += new Emphasis([section:section])
            } else if (["note"].contains(qName)) {
                sectionStack[-1].chunks += new Note([section:section])
            } else if (["important"].contains(qName)) {
                sectionStack[-1].chunks += new Important([section:section])
            } else if (qName == "imagedata") {
                sectionStack[-1].chunks += new ImageData([section:section])
            } else if (qName == "imageobject") {
                sectionStack[-1].chunks += new ImageObject([section:section])
            } else if (qName == "mediaobject") {
                sectionStack[-1].chunks += new MediaObject([section:section])
            } else if (qName == "screenshot") {
                sectionStack[-1].chunks += new Screenshot([section:section])
            } else if (qName == "term") {
                sectionStack[-1].chunks += new Term([section:section])
            } else if (qName == "listitem") {
                sectionStack[-1].chunks += new ListItem([section:section])
            } else if (qName == "varlistentry") {
                sectionStack[-1].chunks += new VarListEntry([section:section])
            } else if (qName == "variablelist") {
                sectionStack[-1].chunks += new VariableList([section:section])
            } else if (qName == "orderedlist") {
                sectionStack[-1].chunks += new OrderedList([section:section])
            } else if (qName == "itemizedlist") {
                sectionStack[-1].chunks += new ItemizedList([section:section])
            } else if (qName == "xi:include") {
                sectionStack[-1].chunks += new Include([section:section])
            } else if (qName == "firstname") {
                sectionStack[-1].chunks += new Firstname([section:section])
            } else if (qName == "surname") {
                sectionStack[-1].chunks += new Surname([section:section])
            } else if (qName == "author") {
                sectionStack[-1].chunks += new Author([section:section])
            } else if (qName == "authorgroup") {
                sectionStack[-1].chunks += new AuthorGroup([section:section])
            } else if (["releaseinfo", "date", "legalnotice", "bookinfo", "toc", "titleabbrev", "productname", "affiliation", "year", "copyright", "holder", "xi:fallback", "affiliation", "jobtitle", "orgname", "email", "pubdate"].contains(qName)) {
                // ignore
            } else if(["lineannotation"].contains(qName)) {
                sectionStack[-1].chunks += new PlainText([section:section])
            } else if (["quote"].contains(qName)){
                sectionStack[-1].chunks += new Quote([section:section])
            } else if (qName == "table") {
                sectionStack[-1].chunks += new Table([section:section])
            } else if (qName == "tgroup") {
                sectionStack[-1].chunks += new TableGroup([section:section])
            } else if (qName == "colspec") {
                sectionStack[-1].chunks += new ColumnSpec([section:section])
            } else if (qName == "thead") {
                sectionStack[-1].chunks += new TableHead([section:section])
            } else if (qName == "row") {
                sectionStack[-1].chunks += new Row([section:section])
            } else if (qName == "entry") {
                sectionStack[-1].chunks += new Entry([section:section])
            } else if (qName == "tbody") {
                sectionStack[-1].chunks += new TableBody([section:section])
            } else if (qName == "bridgehead") {
                sectionStack[-1].chunks += new Bridgehead([section:section])
            }
            else {
                throw new RuntimeException("Cannot parse ${qName}")
            }
            log.info("POP ${qName}: Top of sectionStack is now ${sectionStack[-1]}")
        }
    }
    
    private String strip(String input) {
        input.replaceAll('\\s+', ' ')
    }
    
    void endDocument() {
        log.info("You need to unpack ${rootSection}")
        asciidoc = rootSection.render()
        log.info("Printing out your shiny, new asciidoc content...")
        asciidoc.eachLine { line ->
            log.info(line)
        }
        log.info("Creating ${doc.name.split('\\.')[0]+'.adoc'}...")
        new File(doc.name.split('\\.')[0] + '.adoc').withWriter("UTF-8") { out ->
            asciidoc.eachLine { line ->
                out.writeLine(line)
            }
        }
    }
    
    def parse() {
        log.info "Parsing ${doc}.."
        def reader = SAXParserFactory.newInstance().newSAXParser().XMLReader
        reader.setContentHandler(this)
        def inputSource = new InputSource(
            new InputStreamReader(
                new FileInputStream(doc), 
                "UTF-8"
            )
        )
        inputSource.encoding = "UTF-8"
        reader.parse(inputSource)
    }   
    
}

@Log
class Runner implements CommandLineRunner {

    void run(String[] args) {
        args.each { arg ->
            log.info "You want to convert ${arg}?"
            def input = new File(arg)
            new Docbook5Handler(input).parse()
        }
    }
}
