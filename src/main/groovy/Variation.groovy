package wcexport

import groovy.sql.Sql
import groovy.util.logging.Log4j

import javax.sql.DataSource

import static wcexport.Product.initialParse
import static wcexport.Product.makeCamelCase
import static wcexport.Product.parseString

/**
 * Created by Michael van Niekerk on 2016-05-10.
 */
@Log4j
class Variation {

    def attributes = [:]
    def id

    BigDecimal getPrice() {
        attributes.price ? new BigDecimal(attributes.price) : new BigDecimal(0)
    }

    BigDecimal getSalePrice() {
        attributes.salePrice ? new BigDecimal(attributes.salePrice) : getPrice()
    }

    boolean isInStock() {
        attributes.stockStatus && attributes.stockStatus == 'instock'
    }

    BigDecimal getStock() {
        attributes.stock ? new BigDecimal(attributes.stock) : new BigDecimal(0)
    }

    Map<String, String> getVariationDescription() {
        attributes.keySet().findAll {String s -> s.startsWith "attribute"}.collectEntries { String key ->
            def s = key - 'attributes'
            s = s[0].toLowerCase() + (s.size() > 1 ? s[1..-1] : "")
            [(s): attributes[key]]
        }
    }

    Map toMap() {
        [
                id: id,
                price: price,
                salePrice: salePrice,
                inStock: inStock,
                stock: stock,
                description: variationDescription,
        ]
    }

    static Variation getVariationForId(def id, DataSource dataSource) {

        def ret = new Variation()

        def sql = new Sql(dataSource)
        try {
            sql.eachRow "select * from wp_postmeta where post_id=${id}", { meta ->
                String key = makeCamelCase meta.meta_key
                ret.attributes[(key)] = meta.meta_value
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            sql.close()
        }

        println "\t$ret.attributes"
        ret
    }


}
