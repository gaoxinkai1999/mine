import {BleClient} from '@capacitor-community/bluetooth-le';
import {showLoadingToast, showSuccessToast, showFailToast} from 'vant';
import iconv from 'iconv-lite';
import 'vant/es/toast/style';

const SERVICE_UUID = '000018f0-0000-1000-8000-00805f9b34fb';
const CHARACTERISTIC_UUID = '00002af1-0000-1000-8000-00805f9b34fb';
const TARGET_DEVICE_NAME = 'MPT-II';

// 初始化蓝牙
async function initializeBluetooth() {
    try {
        await BleClient.initialize();
        console.log("蓝牙初始化成功");
    } catch (err) {
        console.log("蓝牙初始化失败:", err.message);
        showFailToast("蓝牙初始化失败,请重试!");
        throw new Error(err);
    }
}

// 扫描指定名称的设备
async function scanForDeviceByName(targetName) {
    try {
        let foundDevice = null;

        // 开始扫描前先确保停止之前的扫描
        try {
            await BleClient.stopLEScan();
        } catch (e) {
            console.log('No active scanning to stop');
        }

        return new Promise((resolve, reject) => {
            let timeoutId;

            const scanCallback = (result) => {
                console.log('Scanned device:', result);
                if (result.device && result.device.name === targetName) {
                    foundDevice = result.device;
                    BleClient.stopLEScan();
                    clearTimeout(timeoutId);
                    resolve(foundDevice);
                }
            };

            // 开始扫描
            BleClient.requestLEScan(
                {
                    services: [],
                    namePrefix: '',
                    allowDuplicates: true
                },
                scanCallback
            ).catch(error => {
                console.error('Scan error:', error);
                reject(error);
            });

            // 设置超时
            timeoutId = setTimeout(async () => {
                await BleClient.stopLEScan();
                if (foundDevice) {
                    resolve(foundDevice);
                } else {
                    reject(new Error(`未找到名称为 ${targetName} 的设备`));
                }
            }, 5000);
        });
    } catch (error) {
        console.error('Scan error:', error);
        throw error;
    }
}

// 连接设备
async function connectDevice(device) {
    try {
        await BleClient.connect(device.deviceId, (disconnectData) => {
            console.log("设备已断开连接", disconnectData);
            showFailToast("设备连接断开");
        });

        await new Promise(resolve => setTimeout(resolve, 1000));

        console.log("设备连接成功");
        return {
            deviceId: device.deviceId,
            service: SERVICE_UUID,
            characteristic: CHARACTERISTIC_UUID
        };
    } catch (err) {
        console.log("设备连接失败:", err.message);
        showFailToast("设备连接失败,请重试!");
        throw new Error(err);
    }
}

// 连接到指定名称的设备
async function connectToDeviceByName() {
    try {
        await initializeBluetooth();

        console.log("开始扫描设备...");
        showLoadingToast({
            message: '正在搜索设备...',
            forbidClick: true,
            duration: 0
        });

        const device = await scanForDeviceByName(TARGET_DEVICE_NAME);


        if (device) {
            showLoadingToast({
                message: '正在连接设备...',
                forbidClick: true,
                duration: 0
            });

            const connection = await connectDevice(device);

            showSuccessToast("设备连接成功");
            return connection;
        } else {
            showFailToast("未找到指定名称的设备");
            return null;
        }
    } catch (err) {

        console.error("连接设备失败:", err);
        showFailToast(err.message || "连接设备失败,请重试!");
        throw err;
    }
}

// 发送打印数据
async function sendPrinterData(characteristic, cmd) {
    if (!characteristic) {
        console.log("打印失败:特性为空");
        throw new Error("打印机未连接");
    }

    // eslint-disable-next-line no-useless-catch
    try {
        const encodedCmd = iconv.encode(cmd, 'gbk');
        const maxChunkSize = 128;
        const chunks = [];

        for (let i = 0; i < encodedCmd.length; i += maxChunkSize) {
            chunks.push(encodedCmd.slice(i, i + maxChunkSize));
        }

        const totalChunks = chunks.length;
        for (let i = 0; i < totalChunks; i++) {
            const chunk = chunks[i];
            try {
                await BleClient.write(
                    characteristic.deviceId,
                    characteristic.service,
                    characteristic.characteristic,
                    chunk
                );
                console.log(`发送块 ${i + 1}/${totalChunks}`);
            } catch (err) {
                console.log("发送失败:", err.message);
                throw new Error("打印数据发送失败:" + err.message);
            }
        }

        return true;
    } catch (error) {
        throw error;
    }
}

// 格式化打印小票
export function formatReceipt(order, isPrinting) {
    const {shop, createTime, totalSalesAmount, orderDetails} = order;
    
    // 使用公共的页眉函数
    let receipt = addHeader(shop, isPrinting);
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        receipt += formatText(`日期: ${createTime}\n`, { size: 0 });
        receipt += formatText("--------------------------------\n", { size: 0 });
        receipt += formatText(padRight("商品名称", 14) + padRight("数量", 6) + padRight("单价", 8) + padRight("总价", 8) + '\n', { size: 0, bold: true });
    } else {
        receipt += `日期: ${createTime}\n`;
        receipt += "--------------------------------\n";
        receipt += padRight("商品名称", 14) + padRight("数量", 6) + padRight("单价", 8) + padRight("总价", 8) + '\n';
    }
    
    // 使用公共的商品格式化函数
    orderDetails.forEach(detail => {
        receipt += formatSaleProductRow(detail, isPrinting);
    });
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        receipt += formatText("--------------------------------\n", { size: 0 });
        receipt += formatText(`总计: ${totalSalesAmount}\n\n\n\n`, { size: 0, bold: true });
    } else {
        receipt += "--------------------------------\n";
        receipt += `总计: ${totalSalesAmount}\n\n\n\n`;
    }
    
    return receipt;
}

// 格式化退货单打印内容
export function formatReturnReceipt(order, isPrinting) {
    const {shop, createTime, amount, returnOrderDetails} = order;
    
    // 使用公共的页眉函数，包含"退货单"标题
    let receipt = addHeader(shop, isPrinting, "退货单");
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        receipt += formatText(`日期: ${formatTime(createTime)}\n`, { size: 0 });
        receipt += formatText("--------------------------------\n", { size: 0 });
        receipt += formatText(padRight("商品名称", 14) + padRight("类型", 8) + padRight("数量", 6) + padRight("金额", 8) + '\n', { size: 0, bold: true });
    } else {
        receipt += `日期: ${formatTime(createTime)}\n`;
        receipt += "--------------------------------\n";
        receipt += padRight("商品名称", 14) + padRight("类型", 8) + padRight("数量", 6) + padRight("金额", 8) + '\n';
    }
    
    // 使用公共的退货商品格式化函数
    returnOrderDetails.forEach(detail => {
        receipt += formatReturnProductRow(detail, isPrinting);
    });
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        receipt += formatText("--------------------------------\n", { size: 0 });
        receipt += formatText(`退款总额: ${amount}\n\n\n\n`, { size: 0, bold: true });
    } else {
        receipt += "--------------------------------\n";
        receipt += `退款总额: ${amount}\n\n\n\n`;
    }
    
    return receipt;
}

// 格式化合并订单打印内容
export function formatMergedReceipt(orders, isPrinting) {
    if (!orders || orders.length === 0) return "";
    
    // 确保所有订单来自同一个商店
    const shopId = orders[0].shop.id;
    if (!orders.every(order => order.shop.id === shopId)) {
        throw new Error("合并订单必须来自同一个商店");
    }
    
    const shop = orders[0].shop;
    
    // 使用公共的页眉函数，包含"合并订单"标题
    let receipt = addHeader(shop, isPrinting, "合并订单");
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        receipt += formatText(`打印时间: ${formatTime(new Date())}\n`, { size: 0 });
        receipt += formatText("--------------------------------\n", { size: 0 });
    } else {
        receipt += `打印时间: ${formatTime(new Date())}\n`;
        receipt += "--------------------------------\n";
    }
    
    // 订单汇总统计
    let totalSalesAmount = 0;
    let totalReturnAmount = 0;
    
    // 分类处理销售订单和退货订单
    const salesOrders = orders.filter(order => order.orderDetails);
    const returnOrders = orders.filter(order => order.returnOrderDetails);
    
    // 处理销售订单
    if (salesOrders.length > 0) {
        if (isPrinting) {
            receipt += formatText("\n【销售订单】\n", { size: 0, bold: true });
            receipt += formatText(padRight("商品名称", 14) + padRight("数量", 6) + padRight("单价", 8) + padRight("总价", 8) + '\n', { size: 0, bold: true });
        } else {
            receipt += "\n【销售订单】\n";
            receipt += padRight("商品名称", 14) + padRight("数量", 6) + padRight("单价", 8) + padRight("总价", 8) + '\n';
        }
        
        salesOrders.forEach(order => {
            order.orderDetails.forEach(detail => {
                receipt += formatSaleProductRow(detail, isPrinting);
            });
            totalSalesAmount += Number(order.totalSalesAmount);
        });
        
        if (isPrinting) {
            receipt += formatText("--------------------------------\n", { size: 0 });
            receipt += formatText(`销售总额: ${totalSalesAmount.toFixed(2)}\n\n`, { size: 0, bold: true });
        } else {
            receipt += "--------------------------------\n";
            receipt += `销售总额: ${totalSalesAmount.toFixed(2)}\n\n`;
        }
    }
    
    // 处理退货订单
    if (returnOrders.length > 0) {
        if (isPrinting) {
            receipt += formatText("\n【退货订单】\n", { size: 0, bold: true });
            receipt += formatText(padRight("商品名称", 14) + padRight("类型", 8) + padRight("数量", 6) + padRight("金额", 8) + '\n', { size: 0, bold: true });
        } else {
            receipt += "\n【退货订单】\n";
            receipt += padRight("商品名称", 14) + padRight("类型", 8) + padRight("数量", 6) + padRight("金额", 8) + '\n';
        }
        
        returnOrders.forEach(order => {
            order.returnOrderDetails.forEach(detail => {
                receipt += formatReturnProductRow(detail, isPrinting);
            });
            totalReturnAmount += Number(order.amount);
        });
        
        if (isPrinting) {
            receipt += formatText("--------------------------------\n", { size: 0 });
            receipt += formatText(`退款总额: ${totalReturnAmount.toFixed(2)}\n\n`, { size: 0, bold: true });
        } else {
            receipt += "--------------------------------\n";
            receipt += `退款总额: ${totalReturnAmount.toFixed(2)}\n\n`;
        }
    }
    
    // 最终汇总
    if (isPrinting) {
        receipt += formatText("================================\n", { size: 0 });
        const netAmount = totalSalesAmount - totalReturnAmount;
        receipt += formatText(`最终结算: ${netAmount.toFixed(2)}\n\n\n\n`, { size: 0, bold: true });
    } else {
        receipt += "================================\n";
        const netAmount = totalSalesAmount - totalReturnAmount;
        receipt += `最终结算: ${netAmount.toFixed(2)}\n\n\n\n`;
    }
    
    return receipt;
}

function padRight(str, length) {
    // eslint-disable-next-line no-control-regex
    const strLength = str.replace(/[^\x00-\xff]/g, 'xx').length;
    const padLength = Math.max(0, length - strLength);
    return str + ' '.repeat(padLength);
}

// 处理文本溢出，将超长文本分割成多行
function handleTextOverflow(text, maxLength) {
    // eslint-disable-next-line no-control-regex
    if (text.replace(/[^\x00-\xff]/g, 'xx').length <= maxLength) {
        return [text];
    }

    const lines = [];
    let currentLine = '';
    let currentLength = 0;

    for (let i = 0; i < text.length; i++) {
        const char = text[i];
        // eslint-disable-next-line no-control-regex
        const charLength = char.match(/[^\x00-\xff]/) ? 2 : 1;

        if (currentLength + charLength > maxLength) {
            lines.push(currentLine);
            currentLine = char;
            currentLength = charLength;
        } else {
            currentLine += char;
            currentLength += charLength;
        }
    }

    if (currentLine) {
        lines.push(currentLine);
    }

    return lines;
}

/**
 * 设置文本的大小和样式
 * @param {string} text - 要格式化的文本
 * @param {Object} options - 格式化选项
 * @param {number} options.size - 字体大小 (0-7): 0=正常, 1=双高, 2=双宽, 3=双高双宽, 4=三倍高, 5=三倍宽, 6=三倍高三倍宽, 7=四倍高四倍宽
 * @param {boolean} options.bold - 是否加粗
 * @param {boolean} options.underline - 是否下划线
 * @param {boolean} options.reverse - 是否反白
 * @returns {string} 格式化后的文本
 */
function formatText(text, options = {}) {
    const defaultOptions = {
        size: 0,      // 默认正常大小
        bold: false,  // 默认不加粗
        underline: false, // 默认无下划线
        reverse: false    // 默认不反白
    };

    const opts = {...defaultOptions, ...options};

    let commands = '';

    // 字体大小设置命令 ESC ! n
    // n的值取决于大小:
    // 0: 正常
    // 1: 双高
    // 16: 双宽
    // 17: 双高双宽
    // ... 等等
    let sizeValue = 0;
    switch (opts.size) {
        case 0:
            sizeValue = 0;
            break;    // 正常
        case 1:
            sizeValue = 1;
            break;    // 双高
        case 2:
            sizeValue = 16;
            break;   // 双宽
        case 3:
            sizeValue = 17;
            break;   // 双高双宽
        case 4:
            sizeValue = 32;
            break;   // 三倍高
        case 5:
            sizeValue = 48;
            break;   // 三倍宽
        case 6:
            sizeValue = 49;
            break;   // 三倍高三倍宽
        case 7:
            sizeValue = 64;
            break;   // 四倍高四倍宽
        default:
            sizeValue = 0;
    }

    commands += '\x1B\x21' + String.fromCharCode(sizeValue);

    // 加粗命令 ESC E n
    if (opts.bold) {
        commands += '\x1B\x45\x01';  // 加粗
    } else {
        commands += '\x1B\x45\x00';  // 取消加粗
    }

    // 下划线命令 ESC - n
    if (opts.underline) {
        commands += '\x1B\x2D\x01';  // 添加下划线
    } else {
        commands += '\x1B\x2D\x00';  // 取消下划线
    }

    // 反白命令 GS B n
    if (opts.reverse) {
        commands += '\x1D\x42\x01';  // 反白
    } else {
        commands += '\x1D\x42\x00';  // 取消反白
    }

    return commands + text;
}

// 保持向后兼容的enlargeText函数
function enlargeText(text) {
    return formatText(text, {size: 3, bold: true});  // 双高双宽并加粗
}

/**
 * 添加页眉，包括店名、电话、商店名等信息
 * @param {Object} shop - 商店信息
 * @param {boolean} isPrinting - 是否是打印模式
 * @param {string} [title] - 可选的标题，如"退货单"、"合并订单"等
 * @returns {string} 格式化后的页眉
 */
function addHeader(shop, isPrinting, title = null) {
    let header = "\n\n";
    
    // 添加店名和电话
    if (isPrinting) {
        header += formatText("清徐欣凯副食经销", { size: 6, bold: true }) + "\n";
        header += formatText("电话: 13912345678\n\n", { size: 0 });
        header += formatText(shop.name, { size: 3, bold: true }) + "\n\n";
        if (title) {
            header += formatText(title, { size: 3, bold: true }) + "\n\n";
        }
    } else {
        // 非打印模式下使用纯文本
        header += "清徐欣凯副食经销\n";
        header += "电话: 13912345678\n\n";
        header += `${shop.name}\n\n`;
        if (title) {
            header += `${title}\n\n`;
        }
    }
    
    return header;
}

/**
 * 格式化销售订单的商品行
 * @param {Object} detail - 商品详情
 * @param {boolean} isPrinting - 是否是打印模式
 * @returns {string} 格式化后的商品行
 */
function formatSaleProductRow(detail, isPrinting = true) {
    let row = '';
    const productName = detail.product.name;
    const nameLines = handleTextOverflow(productName, 10); // 缩短名称列宽度，留出间距
    
    // 处理第一行
    const name = padRight(nameLines[0], 14); // 增加填充长度，确保有间距
    const num = padRight(String(detail.quantity), 6);
    const price = padRight(String(detail.salePrice), 8);
    const total = padRight(String(detail.totalSalesAmount), 8);
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        row += formatText(`${name}${num}${price}${total}\n`, { size: 0 });
    } else {
        row += `${name}${num}${price}${total}\n`;
    }
    
    // 处理溢出的文本行
    for (let i = 1; i < nameLines.length; i++) {
        const overflowText = `${padRight(nameLines[i], 14)}${padRight("", 6)}${padRight("", 8)}${padRight("", 8)}\n`;
        if (isPrinting) {
            row += formatText(overflowText, { size: 0 });
        } else {
            row += overflowText;
        }
    }
    
    return row;
}

/**
 * 格式化退货订单的商品行
 * @param {Object} detail - 商品详情
 * @param {boolean} isPrinting - 是否是打印模式
 * @returns {string} 格式化后的商品行
 */
function formatReturnProductRow(detail, isPrinting = true) {
    let row = '';
    const productName = detail.product.name;
    const nameLines = handleTextOverflow(productName, 10); // 缩短名称列宽度，留出间距
    
    // 处理第一行
    const name = padRight(nameLines[0], 14); // 增加填充长度，确保有间距
    const type = padRight(detail.type, 8);
    // 如果是退货退款则显示数量，否则显示"-"
    const num = detail.type === '退货退款' ? padRight(String(detail.quantity || 1), 6) : padRight("-", 6);
    const total = padRight(String(detail.amount), 8);
    
    // 根据模式决定是否使用formatText
    if (isPrinting) {
        row += formatText(`${name}${type}${num}${total}\n`, { size: 0 });
    } else {
        row += `${name}${type}${num}${total}\n`;
    }
    
    // 处理溢出的文本行 - 确保与主行对齐一致
    for (let i = 1; i < nameLines.length; i++) {
        const overflowText = `${padRight(nameLines[i], 14)}${padRight("", 8)}${padRight("", 6)}${padRight("", 8)}\n`;
        if (isPrinting) {
            row += formatText(overflowText, { size: 0 });
        } else {
            row += overflowText;
        }
    }
    
    return row;
}

// 格式化时间
function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

// 断开连接函数
async function disconnectDevice(deviceId) {
    if (!deviceId) return;

    try {
        // 使用 try-catch 捕获可能的错误,但不弹出 Toast
        await BleClient.disconnect(deviceId);
        console.log("设备已成功断开连接");
    } catch (error) {
        // 仅记录日志,不显示错误弹窗
        console.error("断开连接时发生错误:", error);
    }
}

export async function printOrder(order, onStatusChange = () => {
}) {
    let deviceId = null;
    try {
        onStatusChange('连接打印机...');

        // 连接设备
        const device = await connectToDeviceByName();
        deviceId = device.deviceId;

        // 打印
        onStatusChange('正在打印...');
        const printContent = formatReceipt(order, true);
        await sendPrinterData(device, printContent);

        // 打印成功后主动断开连接
        onStatusChange('断开连接...');
        await disconnectDevice(deviceId);

        // 成功提示
        showSuccessToast('打印成功');
    } catch (error) {
        console.error('打印过程出错:', error);

        // 如果连接过,尝试断开连接
        if (deviceId) {
            await disconnectDevice(deviceId);
        }

        // 错误提示
        showFailToast(error.message || '打印失败');
        throw error;
    } finally {
        onStatusChange('');
    }
}

// 打印退货订单
export async function printReturnOrder(order, onStatusChange = () => {
}) {
    let deviceId = null;
    try {
        onStatusChange('连接打印机...');

        // 连接设备
        const device = await connectToDeviceByName();
        deviceId = device.deviceId;

        // 打印
        onStatusChange('正在打印...');
        const printContent = formatReturnReceipt(order, true);
        await sendPrinterData(device, printContent);

        // 打印成功后主动断开连接
        onStatusChange('断开连接...');
        await disconnectDevice(deviceId);

        // 成功提示
        showSuccessToast('打印成功');
    } catch (error) {
        console.error('打印过程出错:', error);

        // 如果连接过,尝试断开连接
        if (deviceId) {
            await disconnectDevice(deviceId);
        }

        // 错误提示
        showFailToast(error.message || '打印失败');
        throw error;
    } finally {
        onStatusChange('');
    }
}

// 打印合并订单
export async function printMergedOrders(orders, onStatusChange = () => {
}) {
    let deviceId = null;
    try {
        onStatusChange('连接打印机...');

        // 连接设备
        const device = await connectToDeviceByName();
        deviceId = device.deviceId;

        // 打印
        onStatusChange('正在打印合并订单...');
        const printContent = formatMergedReceipt(orders, true);
        await sendPrinterData(device, printContent);

        // 打印成功后主动断开连接
        onStatusChange('断开连接...');
        await disconnectDevice(deviceId);

        // 成功提示
        showSuccessToast('合并打印成功');
    } catch (error) {
        console.error('打印过程出错:', error);

        // 如果连接过,尝试断开连接
        if (deviceId) {
            await disconnectDevice(deviceId);
        }

        // 错误提示
        showFailToast(error.message || '合并打印失败');
        throw error;
    } finally {
        onStatusChange('');
    }
}