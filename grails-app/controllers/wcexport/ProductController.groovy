package wcexport


import grails.rest.*
import grails.converters.*
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell

class ProductController {

    ProductService productService

    def list() {
        def l = [] as List<Product>
        try {
            l = productService.productList
        } catch (Exception e) {
            e.printStackTrace()
            log.error e.message
        }
        withFormat {
            excel {
                response.setHeader "Content-disposition", "attachment; filename=${new Date()}.xlsx"
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                def wb = new HSSFWorkbook()
                def s = wb.createSheet 'Products'
                //Header row
                def hr = s.createRow 0
                ['ID', 'Name', 'Variation', 'Price', 'In Stock', 'Stock', 'MinPrice', 'MaxPrice', 'Link', 'Categories']
                        .eachWithIndex{ String entry, int i ->
                    def cell = hr.createCell i, Cell.CELL_TYPE_STRING
                    cell.setCellValue entry
                }
                l.eachWithIndex { Product p, int i ->
                    def r = s.createRow(i+1)
                    hr.createCell 0, Cell.CELL_TYPE_NUMERIC setCellValue p.id
                    hr.createCell 2, Cell.CELL_TYPE_NUMERIC setCellValue p.price
                    hr.createCell 3, Cell.CELL_TYPE_BOOLEAN setCellValue p.inStock
                    hr.createCell 4, Cell.CELL_TYPE_NUMERIC setCellValue p.stock
                    hr.createCell 5, Cell.CELL_TYPE_NUMERIC setCellValue p.minPrice
                    hr.createCell 6, Cell.CELL_TYPE_NUMERIC setCellValue p.maxPrice
                    hr.createCell 7, Cell.CELL_TYPE_STRING setCellValue p.url
                    hr.createCell 8, Cell.CELL_TYPE_STRING setCellValue p.categories.join(',')
                }
                wb.write response.outputStream
            }
            json { render l*.toMap() as JSON}
            xml {render l*.toMap() as XML}
            '*' {render l*.toMap() as JSON}
        }
    }

}
