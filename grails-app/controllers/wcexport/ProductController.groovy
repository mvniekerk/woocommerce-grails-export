package wcexport


import grails.rest.*
import grails.converters.*

class ProductController {

    ProductService productService

    def list() {
        def l = productService.productList
        withFormat {
            json { render l as JSON}
            xml {render l as XML}
            '*' {render l as JSON}
        }
    }

}
