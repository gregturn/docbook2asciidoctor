import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*

class Chunk {
    def qName = ""
    def content = "" // put characters() stuff
    def data // put anything else
    def attrs
    String toString() {
        if (data != null) {
            "qName:${qName} data:${data} content:${content} attrs:${attrs}"
        } else {
            "qName:${qName} content: ${content} attrs:${attrs}"
        }
    }
}

class Section {
    def qName, level, chunks=[], attrs=[:]
    
    String toString() {
        def results = "Section> chunks:${chunks} level:${level}"
        attrs.each { key,value ->
            results += "\n${key} => ${value}"
        }
        results
    }
    
    String render() {
        def results = "${'='*level} ${attrs['title'].content.join(" ")}\n"
        
        if (attrs['author'] != null) {
            results += attrs['author'].join(", ") + "\n"
        }
        if (attrs['year'] != null) {
            results += ":year: ${attrs['year'].content.join(" ")}\n"
        }
        if (attrs['legalnotice'] != null) {
            results += ":legalnotice: ${attrs['legalnotice'].chunks.join(" ")}\n"        
        }
        if (attrs['releaseinfo'] != null) {
            results += ":releaseinfo: ${attrs['releaseinfo'].content.join(" ")}\n"
        }
        if (attrs['toc'] != null) {
            results += ":toc:\n"
        }
        
        results += "\n"
        
        chunks.each { chunk ->
            results += chunk.render()
        }
        
        results
    }
}

class Include {
    def chunk
    
    String render() {
        "${toString()}\n"
    }
    
    String toString() {
        "include::${chunk.attrs['href']-'xml'+'ad'}[]"
    }
}

@Log
class Docbook5Handler extends DefaultHandler {

    File doc    
    def qNameStack = []
    def sectionStack = []
    def asciidoc = ""
    def level = 0
    def rootSection
    
    Docbook5Handler() {
    }
    
    Docbook5Handler(File doc) {
        this.doc = doc
    }
    
    void startElement(String ns, String localName, String qName, Attributes attrs) {
        if (qName == "book") {
            qNameStack.push(new Chunk([qName:qName]))
            rootSection = new Section([level:1])
            sectionStack.push(rootSection)
            log.info("PUSH: Top of qNameStack is now ${qNameStack[-1]}")
            log.info("PUSH: sectionStack is now ${sectionStack}")
        } else if (qName == "legalnotice") {
            qNameStack.push(new Chunk([qName:qName]))
            sectionStack.push(new Section([level:sectionStack[-1].level+1]))
            log.info("PUSH: Top of qNameStack is now ${qNameStack[-1]}")
            log.info("PUSH: sectionStack is now ${sectionStack}")
        } else {
            def newattrs = [:]
            if (qName == "xi:include") {
                newattrs['href'] = attrs.getValue("href")
            }            
            qNameStack.push(new Chunk([qName:qName, attrs:newattrs]))
            log.info("PUSH: Top of qNameStack is now ${qNameStack[-1]}")
        }
    }
    
    void characters(char[] chars, int offset, int length) {
        if (qNameStack.size() > 0) {
            if (qNameStack[-1].qName != "programlisting") {
                qNameStack[-1].content += strip(new String(chars, offset, length))
            } else {
                qNameStack[-1].content += new String(chars, offset, length)
            }
            //log.info("${qNameStack[-1]}")
        }
    }
    
    void endElement(String ns, String localName, String qName) {
        if (qName == "book") {
            level -= 1
            def item = qNameStack.pop()
            log.info("POP: Popped ${item}")
            log.info("POP: Top of sectionStack is now ${sectionStack[-1]}")
        } else if (qName == "legalnotice") {
            def item = qNameStack.pop()
            def legalSection = sectionStack.pop()
            log.info("legalnotice POP: Pulled off ${legalSection}...")
            sectionStack[-1].attrs[qName] = legalSection
        } else {
            def item = qNameStack.pop()
            log.info("POP: Popped ${item}")
            log.info("POP: Top of qNameStack is now ${qNameStack[-1]}")
            log.info("POP: Top of sectionStack = ${sectionStack[-1]}")
            if (item.qName == "firstname") {
                if (sectionStack[-1].attrs["author"] == null) {
                    sectionStack[-1].attrs["author"] = [item.content]
                } else {
                    sectionStack[-1].attrs["author"] += item.content
                }
            } else if (qName == "surname") {
                sectionStack[-1].attrs["author"][-1] += " ${item.content}"
            } else if (["authorgroup", "personname", "author"].contains(qName)) {
                // drop
            } else if (["para"].contains(qName)) {
                sectionStack[-1].chunks += item.content
            } else if (qName == "xi:include") {
                sectionStack[-1].chunks += new Include([chunk:item])
            } else if (sectionStack[-1].attrs[item.qName] == null) {
                sectionStack[-1].attrs[item.qName] = [item]
            } else {
                sectionStack[-1].attrs[item.qName] += item
            }
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
        log.info("Creating ${doc.name.split('\\.')[0]+'.ad'}...")
        new File(doc.name.split('\\.')[0] + '.ad').withWriter("UTF-8") { out ->
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
    
    void setLevel(int level) {
        log.info("Up the initial level to 1")
        this.level = level
    } 
    
}

@Log
class Runner implements CommandLineRunner {

    void run(String[] args) {
        args.each { arg ->
            log.info "You want to convert ${arg}?"
            def input = new File(arg)
            def handler = new Docbook5Handler(input)
            if (!input.name.startsWith("index")) {
                handler.level = 1
            }
            handler.parse()
        }
    }
}