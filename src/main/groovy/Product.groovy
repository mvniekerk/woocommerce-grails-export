package wcexport

import groovy.sql.Sql
import groovy.util.logging.Log4j

import javax.sql.DataSource

/**
 * Created by mike on 2016-05-10.
 */

@Log4j
class Product {

    def id
    String title

    List<Variation> variations = []
    List<String> categories = []
    def attributes = [:]

    BigDecimal getPrice() {
        attributes.price ? new BigDecimal(attributes.price) : new BigDecimal(0)
    }

    BigDecimal getMinPrice() {
        attributes.productAttributes.minVariationPrice ? new BigDecimal(attributes.productAttributes.minVariationPrice) : price
    }

    BigDecimal getMaxPrice() {
        attributes.productAttributes.minVariationPrice ? new BigDecimal(attributes.productAttributes.minVariationPrice) : price
    }

    boolean isInStock() {
        attributes.stockStatus && attributes.stockStatus == 'instock'
    }

    BigDecimal getStock() {
        attributes.stock ? new BigDecimal(attributes.stock) : (BigDecimal)variations.collect {it.stock}.sum()
    }

    String getDescription() {
        attributes.variationDescription ?: ""
    }

    //TODO hard code base URL
    Map toMap() {
        [
                id: id,
                title: title,
                price: price,
                minPrice: minPrice,
                maxPrice: maxPrice,
                inStock: inStock,
                stock: stock,
                categories: categories,
                variations: variations*.toMap(),
                url: "http://blog.rozzerstarantulas.co.za/?page_id=$id"
        ]

    }

    static String initialParse(String s) {
        def untilBrace = s[0..(s.indexOf(":", 2)+1)]
        s[(untilBrace.size())..-2]
    }

    static def parseString(String s) {
        def ret = [:]
        def val = [:]
        ret.val = val
        ret.s = new StringBuffer()
        def cur = "!!!"

        while (s && s[0] != "}") {
            //First get key
            assert s.startsWith("s:")
            ret.s << 's:'
            s -= 's:'
            def size = s[0..(s.indexOf(':')-1)]
            ret.s << size << ':'
            s = s[("s:$size:\"".size()-2)..-1]
            cur = s[0..(s.indexOf('";')-1)]
            s = s[("$cur\":").size()..-1]
            ret.s << "\"$cur\";"

            //Another object
            if (s.startsWith('a:')) {
                def untilBrace = s[0..(s.indexOf(":", 2) + 1)]
                ret.s << untilBrace
                s = s[(untilBrace.size())..-1]

                def v = parseString s
                val[(cur)] = v.val
                s = s - v.s
                ret.s << v.s
                //A String
            } else if (s.startsWith('s:')) {
                ret.s << 's:'
                s -= 's:'
                size = s[0..(s.indexOf(':')-1)]
                ret.s << size << ':'
                s = s[("s:$size:\"".size()-2)..-1]
                def stringVal = s[0..(s.indexOf('";')-1)]
                ret.val[(cur)] = stringVal
                ret.s << "\"$stringVal\";"
                stringVal = "${stringVal}\";"
                s = stringVal == s ? "" : s[stringVal.size()..-1]
                //A number
            } else if (s.startsWith('i:')) {
                ret.s << 'i:'
                s -= 'i:'
                def numberVal = s[0..(s.indexOf(';')-1)]
                ret.s << numberVal << ';'
                s = s[(("$numberVal\";").size()-1)..-1]
                ret.val[(cur)] = numberVal.toBigDecimal()
            }
        }

        ret
    }

    static String makeCamelCase(String key) {
        if (key.startsWith("_")) {
            key = key - "_"
        }
        key = key.split("_").collect { s -> s[0].toUpperCase() + (s.size() > 1 ? s[1..-1] : "")}.join("")
        key = key[0].toLowerCase() + (key.size() > 1 ? key[1..-1] : "")
        key
    }

    static Product getProductForId(def id, DataSource dataSource) {
        def ret = new Product()
        def sql = new Sql(dataSource)
        def isVariation = false
        try {
            log.warn "GPFI: $id"
            sql.eachRow "select post_title from wp_posts where ID=$id", { pm ->
                String postTitle = pm.post_title

                if (!postTitle.startsWith('Variation #')) {
                    ret.title = postTitle
                    ret.id = id
                    log.warn "GPFI: T: $ret.title"

                    sql.eachRow "SELECT wp_term_relationships.*,wp_terms.* FROM wp_term_relationships\n" +
                            "\tLEFT JOIN wp_posts  ON wp_term_relationships.object_id = wp_posts.ID\n" +
                            "\tLEFT JOIN wp_term_taxonomy ON wp_term_taxonomy.term_taxonomy_id = wp_term_relationships.term_taxonomy_id\n" +
                            "\tLEFT JOIN wp_terms ON wp_terms.term_id = wp_term_relationships.term_taxonomy_id\n" +
                            "\tWHERE post_type = 'product' AND taxonomy = 'product_cat' \n" +
                            "\tAND  object_id = $id", { m ->
                        ret.categories << m.name
                    }

                    def attributes = ret.attributes
                    sql.eachRow "SELECT wp_posts.ID,wp_posts.post_title,wp_postmeta.* FROM wp_posts \n" +
                            "\tLEFT JOIN wp_postmeta ON wp_posts.ID = wp_postmeta.post_id\n" +
                            "\tWHERE post_type = 'product' \n" +
                            "\tAND wp_posts.ID = $id", { attribs ->
                        String key = makeCamelCase attribs.meta_key
                        attributes[(key)] = attribs.meta_value
                    }
                    if (attributes.productAttributes) {
                        attributes.productAttributes = parseString(initialParse(attributes.productAttributes)).val
                    }
                    if (attributes.defaultAttributes) {
                        attributes.defaultAttributes = parseString(initialParse(attributes.defaultAttributes)).val
                    }

//                    println "$id: $postTitle ${ret.categories} $attributes"
                    //Get variation
                    sql.eachRow "select * from wp_posts where post_parent=$id and post_type='product_variation'", { var ->
//                        log.warn "\tVariation : " + var.post_title

                        ret.variations << Variation.getVariationForId(var.ID, dataSource)

                    }
                } else {
                    isVariation = true
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            sql.close()
        }
        isVariation ? null : ret
    }
}
