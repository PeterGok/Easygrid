package org.grails.plugin.easygrid

import static org.junit.Assert.*
import groovy.time.TimeCategory

/**
 * base class for integration tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
abstract class AbstractServiceTest {

    def easygridService
    def gridDelegate
    def grailsApplication

    def customGridConfig

    def listGridConfig

    //definitions
    def defaultValues

    void setup() {

        GridUtils.addMixins()

        assert grailsApplication?.domainClasses?.size() == 1
//        easygridService.grailsApplication.config?.easygrid = defaultValues
        defaultValues = grailsApplication.config?.easygrid

        //initialize the custom grid
        customGridConfig = generateConfigForGrid {
            id 'authorGrid'
            dataSourceType 'custom'

            roles 'admin'
            securityProvider { grid, oper ->
                if (grid.roles) {
                    if (grid.roles == 'admin') {
                        return true
                    }
                    return false
                }
                return true
            }
            dataProvider {gridConfig, filters, listParams ->
                [
                        [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11)],
                ]
            }
            dataCount {filters ->
                1
            }
            jqgrid {
                width 650
                height 150
            }
            columns {
                'author.id' {
                    type 'id'
                }
                'author.name.label' {
                    property 'name'
                    jqgrid {
                        editable true
                        searchClosure {params ->
                            ilike('name', "%${params.name}%")
                        }
                    }
                    export {
                        width 100
                    }
                }
                'author.nation.label' {
                    property 'nation'
                    jqgrid {
                        searchClosure {params ->
                            ilike('nation', "%${params.nation}%")
                        }
                    }
                }
                'author.age.label' {
                    value { row ->
                        use(TimeCategory) {
                            new Date().year - row.birthDate.time.year
                        }
                    }
                    jqgrid {
                        name 'age'
                        width 110
                        searchClosure {params ->
                            eq('age', params.age as int)
                        }
                    }
                }
                'author.birthDate.label' {
                    property 'birthDate'
                    formatName 'stdDateFormatter'
                    jqgrid {
                        width 110
                        searchClosure {params ->
                            eq('birthDate', params.birthDate)
                        }
                    }
                }
            }
        }

        EasygridContextHolder.session.listData = (1..200).collect {[col1: it, col2: "$it"]}

        //initialize the list grid
        listGridConfig = generateConfigForGrid {
            id 'listProviderGrid'
            dataSourceType 'list'
            context 'session'
            attributeName 'listData'
            columns {
                'list.col1.label' {
                    property 'col1'
                    jqgrid {
                        editable true
                        searchClosure {params, element ->
                            element.col1 > params.min
                        }
                    }
                }
                'list.col2.label' {
                    property 'col2'
                    jqgrid {
                        searchClosure { params, element ->
//                            println '--------'
//                            println element.col2
//                            println params.col2
                            element.col2.contains(params.col2)
                        }
                    }
                }
                'list.col3.label' {
                    value {it.col1 * it.col1}
                    jqgrid {
                        name 'col3'
                        searchClosure { params, element ->
                        }
                    }
                }
            }
        }
    }

    /**
     * generates a config from a grid closure
     * @param gridConfigClosure
     * @return
     */
    def generateConfigForGrid(Closure gridConfigClosure) {
//        def gridConfig = [:]
        def gridConfig = new Grid()
        gridDelegate.gridConfig = gridConfig
        gridConfigClosure.delegate = gridDelegate
        gridConfigClosure.resolveStrategy = Closure.DELEGATE_FIRST
        gridConfigClosure()
        gridConfig
    }


    def populateTestDomain(N = 100) {
//        def N = 100
        (1..N).each {
            new TestDomain(testStringProperty: "$it", testIntProperty: it).save(true)
        }
        assertEquals N, TestDomain.count()
    }

}
