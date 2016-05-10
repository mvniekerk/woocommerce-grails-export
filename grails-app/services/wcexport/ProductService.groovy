package wcexport

import grails.transaction.Transactional
import groovy.sql.Sql

import javax.sql.DataSource

@Transactional
class ProductService {

    //Injected
    DataSource dataSource

    List<Product> getProductList() {
        def sql = new Sql(dataSource)
        def ret = []
        try {
            sql.eachRow '''select post_id from wp_postmeta where meta_key="_price"''', { postRow ->
                def postId = postRow.post_id
                ret << Product.getProductForId(postId, dataSource)
            }
        } finally {
            sql?.close()
        }
        ret?.findAll {it} ?: []
    }

}
