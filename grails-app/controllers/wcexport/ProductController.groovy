package wcexport


import grails.rest.*
import grails.converters.*
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.Hyperlink
import org.apache.poi.ss.usermodel.IndexedColors

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
                response.setHeader "Content-disposition", "attachment; filename=${new Date().format('yyyy-M-dd.hh.mm.ss')}.xls"
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                def wb = new HSSFWorkbook()
                def creationHelper = wb.creationHelper
                def s = wb.createSheet 'Products'

                def linkStyle = wb.createCellStyle()
                def linkFont = wb.createFont()
                linkFont.underline = Font.U_SINGLE
                linkFont.color = IndexedColors.BLUE.index
                linkStyle.font = linkFont

                //Header row
                def hr = s.createRow 0
                ['ID', 'Name', 'Variation', 'Price', 'In Stock', 'Stock', 'MinPrice', 'MaxPrice', 'Link', 'Categories']
                        .eachWithIndex{ String entry, int i ->
                    def cell = hr.createCell i, Cell.CELL_TYPE_STRING
                    cell.setCellValue entry
                }

                def extra = 1
                l.eachWithIndex { Product p, int i ->
                    def r = s.createRow(i+extra)
                    r.createCell 0, Cell.CELL_TYPE_NUMERIC setCellValue p.id
                    r.createCell 1, Cell.CELL_TYPE_STRING setCellValue p.title
                    r.createCell 3, Cell.CELL_TYPE_NUMERIC setCellValue p.price
                    r.createCell 4, Cell.CELL_TYPE_BOOLEAN setCellValue p.inStock
                    r.createCell 5, Cell.CELL_TYPE_NUMERIC setCellValue p.stock
                    r.createCell 6, Cell.CELL_TYPE_NUMERIC setCellValue p.minPrice
                    r.createCell 7, Cell.CELL_TYPE_NUMERIC setCellValue p.maxPrice
                    r.createCell 8, Cell.CELL_TYPE_STRING setCellValue p.url
                    def link = creationHelper.createHyperlink(Hyperlink.LINK_URL)
                    link.address = p.url
                    r.getCell(8).hyperlink = link
                    r.getCell(8).cellStyle = linkStyle

                    r.createCell 9, Cell.CELL_TYPE_STRING setCellValue p.categories.join(',')

                    p.variations.eachWithIndex{ Variation v, int vi ->
                        def rr = s.createRow(i+extra+vi+1)
                        rr.createCell 0, Cell.CELL_TYPE_NUMERIC setCellValue v.id
                        rr.createCell 2, Cell.CELL_TYPE_STRING setCellValue v.name
                        rr.createCell 3, Cell.CELL_TYPE_NUMERIC setCellValue v.price
                        rr.createCell 4, Cell.CELL_TYPE_BOOLEAN setCellValue v.inStock
                        rr.createCell 5, Cell.CELL_TYPE_NUMERIC setCellValue v.stock
                        rr.createCell 6, Cell.CELL_TYPE_NUMERIC setCellValue v.salePrice
                    }
                    extra += p.variations.size()
                }
                wb.write response.outputStream
            }
            json { render l*.toMap() as JSON}
            xml {render l*.toMap() as XML}
            '*' {render l*.toMap() as JSON}
        }
    }

}
