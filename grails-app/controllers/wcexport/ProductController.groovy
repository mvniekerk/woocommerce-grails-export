package wcexport


import grails.rest.*
import grails.converters.*

class ProductController {

    ProductService productService

    def list() {
        def l = []
        try {
            println ">>>>FSDFSFSDF"
            log.warn "Before getting list"
            l = productService.productList*.toMap()
            log.warn l
        } catch (Exception e) {
            e.printStackTrace()
            log.error e.message
        }
        withFormat {
            json { render l as JSON}
            xml {render l as XML}
            '*' {render l as JSON}
        }
    }

}
