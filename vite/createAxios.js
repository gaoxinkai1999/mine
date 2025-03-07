import axios from 'axios'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// Configuration
const config = {
    swaggerUrl: 'http://localhost:8085/api/api-docs',
    outputDir: './src/api',
    // API method template
    methodTemplate: `  /**
   * @description {summary}
   * @url {url}
   * @method {method}
{paramComments}
   * @returns {Promise<{returnType}>}
   */
  {functionName}({parameters}) {
    return request({
      url: {urlTemplate},
      method: '{method}'{requestParams}
    })
  }`,
    // Module file template
    moduleTemplate: `import request from '@/utils/axiosConfig'

const api = {
{methods}
}

export default api`
}

// Generate API files
async function generateApi() {
    try {
        const response = await axios.get(config.swaggerUrl)
        const openApi = response.data

        // Generate objects by module
        const modules = {}

        // Get all valid tags first
        const validTags = (openApi.tags || [])
            .map(tag => tag.name)
            .filter(Boolean)

        console.log('Valid tags:', validTags)

        Object.entries(openApi.paths).forEach(([path, methods]) => {
            Object.entries(methods).forEach(([method, apiInfo]) => {
                if (apiInfo.tags &&
                    apiInfo.tags.length > 0 &&
                    validTags.includes(apiInfo.tags[0])) {
                    const tag = apiInfo.tags[0]
                    if (!modules[tag]) {
                        modules[tag] = []
                    }
                    modules[tag].push(generateApiMethod(path, method, apiInfo))
                }
            })
        })

        // Generate module files
        for (const [tag, methods] of Object.entries(modules)) {
            if (methods.length > 0) {
                const fileName = formatModuleName(tag)
                const content = config.moduleTemplate.replace(
                    '{methods}',
                    methods.join(',\n\n')
                )

                await fs.promises.writeFile(
                    path.join(config.outputDir, `${fileName}.js`),
                    content
                )
            }
        }

        // Generate unified export file
        const validModules = Object.keys(modules).filter(tag => modules[tag].length > 0)
        if (validModules.length > 0) {
            await generateIndexFile(validModules)
        }

        console.log('API generation complete!')
        console.log('Generated modules:', Object.keys(modules))
    } catch (error) {
        console.error('Generation failed:', error)
    }
}

// Format module name
function formatModuleName(name) {
    return name.toLowerCase()
        .replace(/-controller$/, '')
        .replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())
}

// Generate function name
function getFunctionName(path, method, apiInfo) {
    if (apiInfo.operationId) {
        return apiInfo.operationId.replace(/_\d+$/, '')
    }

    const parts = path.split('/')
    const lastPart = parts[parts.length - 1]
    const cleanName = lastPart.replace(/-./g, x => x[1].toUpperCase())

    return method.toLowerCase() + cleanName.charAt(0).toUpperCase() + cleanName.slice(1)
}

// Generate parameter list
function getParameters(parameters, requestBody) {
    const params = []

    const pathParams = parameters.filter(p => p.in === 'path')
    pathParams.forEach(param => {
        params.push(param.name)
    })

    const queryParams = parameters.filter(p => p.in === 'query')
    if (queryParams.length > 0) {
        params.push('params = {}')
    }

    if (requestBody) {
        params.push('data')
    }

    return params
}

// Generate parameter comments
function generateParamComments(parameters, requestBody) {
    const comments = []

    parameters.filter(p => p.in === 'path').forEach(param => {
        comments.push(`   * @param {${getParamType(param.schema)}} ${param.name} ${param.description || ''}`)
    })

    const queryParams = parameters.filter(p => p.in === 'query')
    if (queryParams.length > 0) {
        comments.push(`   * @param {Object} params Query parameters`)
        queryParams.forEach(param => {
            comments.push(`   * @param {${getParamType(param.schema)}} params.${param.name} ${param.description || ''}`)
        })
    }

    if (requestBody) {
        const schema = requestBody.content['application/json'].schema
        comments.push(`   * @param {Object} data Request body`)
        if (schema.properties) {
            Object.entries(schema.properties).forEach(([key, prop]) => {
                comments.push(`   * @param {${getParamType(prop)}} data.${key} ${prop.description || ''}`)
            })
        }
    }

    return comments.join('\n')
}

// Get parameter type
function getParamType(schema) {
    if (!schema) return 'any'

    switch (schema.type) {
        case 'integer':
            return 'number'
        case 'array':
            return `${getParamType(schema.items)}[]`
        case 'object':
            return 'Object'
        default:
            return schema.type || 'any'
    }
}

// Get return type
function getReturnType(responses) {
    const successResponse = responses['200'] || responses['201']
    if (!successResponse) return 'any'

    const schema = successResponse.content?.['application/json']?.schema
    if (!schema) return 'any'

    if (schema.$ref) {
        return schema.$ref.split('/').pop()
    }

    return getParamType(schema)
}

// Generate request configuration
function generateRequestConfig(path, method, apiInfo) {
    const pathParams = (apiInfo.parameters || []).filter(p => p.in === 'path')
    const queryParams = (apiInfo.parameters || []).filter(p => p.in === 'query')
    const hasRequestBody = !!apiInfo.requestBody

    let urlTemplate
    if (pathParams.length > 0) {
        urlTemplate = '`' + path.replace(/{([^}]+)}/g, '${$1}') + '`'
    } else {
        urlTemplate = `'${path}'`
    }

    let requestParams = ''

    if (queryParams.length > 0) {
        requestParams += ',\n      params'
    }
    if (hasRequestBody) {
        requestParams += ',\n      data'
    }

    return { urlTemplate, requestParams }
}

// Generate unified export file
async function generateIndexFile(moduleNames) {
    const modules = moduleNames.map(name => formatModuleName(name))

    const imports = modules
        .map(name => `import ${name} from './${name}'`)
        .join('\n')

    const exports = `export default {
  ${modules.join(',\n  ')}
}`

    const content = `${imports}\n\n${exports}\n`

    await fs.promises.writeFile(
        path.join(config.outputDir, 'index.js'),
        content
    )
}

// Generate single API method
function generateApiMethod(path, method, apiInfo) {
    const functionName = getFunctionName(path, method, apiInfo)
    const parameters = getParameters(apiInfo.parameters || [], apiInfo.requestBody)
    const paramComments = generateParamComments(apiInfo.parameters || [], apiInfo.requestBody)
    const returnType = getReturnType(apiInfo.responses)
    const { urlTemplate, requestParams } = generateRequestConfig(path, method, apiInfo)

    return config.methodTemplate
        .replace('{summary}', apiInfo.summary || '')
        .replace('{url}', path)
        .replace('{urlTemplate}', urlTemplate)
        .replace(/{method}/g, method.toUpperCase())
        .replace('{paramComments}', paramComments)
        .replace('{returnType}', returnType)
        .replace('{functionName}', functionName)
        .replace('{parameters}', parameters.join(', '))
        .replace('{requestParams}', requestParams)
}

// Run generator
generateApi()

export default generateApi

