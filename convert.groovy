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

@Log
class Section {
    def qName, level, chunks=[], attrs=[:]
    
    String toString() {
        def results = "Section> qName:${qName} chunks:${chunks} level:${level}"
        attrs.each { key,value ->
            results += "\n${key} => ${value}"
        }
        results
    }
    
    String render() {
        def results = ""
        
        if (attrs['xml:id'] != null) {
            results += "[[${attrs['xml:id']}]]\n"
        } else if (attrs['title'] != null) {
            if (attrs['title'].attrs['xml:base'] != [null]) {
                results += "[[${attrs['title'].attrs['xml:base'].join('')}]]\n"
            } else if (attrs['title'].attrs['xml:id'] != [null]) {
                results += "[[${attrs['title'].attrs['xml:id'].join('')}]]\n"
            }
        }
        
        if (attrs['id'] != null) {
            results += "[[${attrs['id']}]]\n"
        }

        if (attrs['title'] != null) {
            results += "${'='*level} ${attrs['title'].content.join(" ")}\n"
        }
        
        if (attrs['author'] != null) {
            results += attrs['author'].join(", ") + "\n"
        }
        if (attrs['year'] != null) {
            results += ":year: ${attrs['year'].content.join(" ")}\n"
        }
        if (attrs['releaseinfo'] != null) {
            results += ":releaseinfo: ${attrs['releaseinfo'].content.join(" ")}\n"
        }
        if (attrs['toc'] != null) {
            results += ":toc:\n"
            results += ":toclevels: 4\n"
            results += ":source-highlighter: prettify\n"
            results += ":idprefix:\n"
        }
        if (attrs['legalnotice'] != null) {
            results += ":legalnotice: ${attrs['legalnotice'].chunks.join(" ")}\n"        
        }
        
        results += "\n"
        
        chunks.each { chunk ->
            if (chunk.metaClass.respondsTo(chunk, "render")) {
                results += chunk.render() + "\n"
            } else {
                results += chunk + "\n"
            }
        }
        
        results
    }
}

class Include {
    def chunk
    
    String render() {
        "${toString()}"
    }
    
    String toString() {
        def revised = chunk.attrs['href']-'xml'+'ad'
        revised = revised.replace('/src/docbkx', '/src/main/asciidoc')
        revised = revised.replace('raw.github.com', 'raw.githubusercontent.com')
        revised = revised.replace('1.9.0.M1', 'issue/DATACMNS-551')
        "include::${revised}[]\n"
    }
}

class Paragraph {
    def content
    
    String render() {
        "${content}\n\n"
    }
    
    String toString() {
        render()
    }
}

@Log
class ProgramListing {
    def content
    def attrs
    
    String render() {
        log.info("${attrs}")
        if (attrs['language'] != null) {
            "[source,${attrs['language']}]\n----\n${content}\n----\n"
        } else {
            "[source]\n----\n${content}\n----\n"
        }
    }
    
    String toString() {
        render()
    }
}

class BulletList {
    def content
    
    String render() {
        def results = ""
        content.each { item ->
            results += "* ${item}"
        }
        results
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
        if (qName == "book") {
            qNameStack.push(new Chunk([qName:qName, attrs:extractedAttrs]))
            rootSection = new Section([qName:qName, attrs:extractedAttrs, level:1])
            sectionStack.push(rootSection)
            log.info("PUSH ${qName}: Top of qNameStack is now ${qNameStack[-1]}")
            log.info("PUSH ${qName}: Top of sectionStack is now ${sectionStack[-1]}")
        } else if (["chapter", "preface", "partintro"].contains(qName)) {
            qNameStack.push(new Chunk([qName:qName, attrs:extractedAttrs]))
            rootSection = new Section([qName:qName, attrs:extractedAttrs, level:2])
            sectionStack.push(rootSection)
            log.info("PUSH ${qName}: Top of qNameStack is now ${qNameStack[-1]}")
            log.info("PUSH ${qName}: Top of sectionStack is now ${sectionStack[-1]}")
        } else if (["legalnotice", "section", "simplesect", "para", "programlisting", "itemizedlist", "listitem", "part"].contains(qName)) {        
            qNameStack.push(new Chunk([qName:qName, attrs:extractedAttrs]))
            sectionStack.push(new Section([qName:qName, level:sectionStack[-1].level+1]))
            log.info("PUSH ${qName}: Top of qNameStack is now ${qNameStack[-1]}")
            log.info("PUSH ${qName}: Top of sectionStack is now ${sectionStack[-1]}")
        } else {
            qNameStack.push(new Chunk([qName:qName, attrs:extractedAttrs]))
            log.info("PUSH ${qName}: Top of qNameStack is now ${qNameStack[-1]}")
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
        def item = qNameStack.pop()
        log.info("POP ${qName}: Popped ${item}")
        if (qNameStack.size() > 0) {
            log.info("POP ${qName}: Top of qNameStack is now ${qNameStack[-1]}")
        }
        log.info("POP ${qName}: Top of sectionStack = ${sectionStack[-1]}")
        if (["book", "chapter", "preface", "partintro"].contains(qName)) {
            // nothing
        } else if (["legalnotice", "section", "simplesect", "para", "programlisting", "itemizedlist", "listitem", "part"].contains(qName)) {
            def section = sectionStack.pop()
            if (qName == "legalnotice") {
                log.info("POP ${qName}: item:${item}")
                sectionStack[-1].attrs[qName] = section  
            } else if (qName == "para") {
                sectionStack[-1].chunks += new Paragraph([content:item.content])
                sectionStack[-1].chunks += section.chunks
            } else if (["section", "simplesect", "part"].contains(qName)) {
                section.attrs += item.attrs
                sectionStack[-1].chunks += section                
                log.info("POP section: Pulled off ${section} and appended it to ${sectionStack[-1].attrs['title']}")
            } else if (qName == "programlisting") {
                sectionStack[-1].chunks += new ProgramListing([content:item.content, attrs:item.attrs])
                log.info("POP section: Pulled off ${section} and appended it to ${sectionStack[-1].attrs['title']}")
                log.info("POP section: Top of sectionStack now looks like ${sectionStack[-1]}")
            } else if (qName == "listitem") {
                log.info("POP ${qName}: ${section}")
                sectionStack[-1].chunks += section.chunks.join("")
            } else if (qName == "itemizedlist") {
                section.chunks.each {
                    log.info("POP ${qName}: ${it}")
                }
                sectionStack[-1].chunks += new BulletList([content:section.chunks])
            }
        } else if (item.qName == "firstname") {
            if (sectionStack[-1].attrs["author"] == null) {
                sectionStack[-1].attrs["author"] = [item.content]
            } else {
                sectionStack[-1].attrs["author"] += item.content
            }
        } else if (qName == "surname") {
            sectionStack[-1].attrs["author"][-1] += " ${item.content}"
        } else if (["authorgroup", "personname", "author"].contains(qName)) {
            // drop
        } else if (qName == "xi:include") {
            sectionStack[-1].chunks += new Include([chunk:item])
        } else if (['code', 'interfacename', 'uri', 'methodname', 'classname'].contains(qName)) {
            log.info("POP ${qName}: ${sectionStack[-1]}")
            log.info("POP ${qName}: ${qNameStack[-1].content}")
            qNameStack[-1].content += "`${item.content}`"
        } else if (qName == "link") {
            log.info("POP ${qName}: attrs is ${item.attrs}")
            if (item.attrs['linkend'] != null) {
                qNameStack[-1].content += "<<${item.attrs['linkend']},${item.content}>>"
            }
            if (item.attrs['xlink:href'] != null) {
                qNameStack[-1].content += "${item.attrs['xlink:href']}[${item.content}]"
            }
        } else if (qName == "ulink") {
            if (item.attrs['url'] != null) {
                qNameStack[-1].content += "${item.attrs['url']}[${item.content}]"
            }
        } else if (sectionStack[-1].attrs[item.qName] == null) {
            sectionStack[-1].attrs[item.qName] = [item]
            log.info("POP ${qName}: Added ${item.qName}/${item.content} to ${sectionStack[-1]}")
        } else {
            sectionStack[-1].attrs[item.qName] += item
            log.info("POP ${qName}: Added ${item.qName}/${item.content} to ${sectionStack[-1]}")
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
