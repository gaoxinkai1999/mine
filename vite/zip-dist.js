// zip-dist.js
import fs from 'fs';
import archiver from 'archiver';

async function zipDirectory() {
    const sourceDir = 'dist';
    const outputFile = 'dist.zip';

    if (!fs.existsSync(sourceDir)) {
        console.error('错误: dist 目录不存在! 请先运行构建命令。');
        process.exit(1);
    }

    const output = fs.createWriteStream(outputFile);
    const archive = archiver('zip', {
        zlib: { level: 9 } // 设置压缩级别
    });

    archive.on('warning', (err) => {
        if (err.code === 'ENOENT') {
            console.warn('警告:', err);
        } else {
            throw err;
        }
    });

    archive.on('error', (err) => {
        throw err;
    });

    archive.pipe(output);
    archive.directory(sourceDir, false);

    await archive.finalize();

    return new Promise((resolve, reject) => {
        output.on('close', () => {
            const fileSize = (archive.pointer() / 1024 / 1024).toFixed(2);
            console.log(`✅ 成功创建 ${outputFile} (${fileSize} MB)`);
            resolve();
        });

        output.on('error', (err) => {
            console.error('创建 zip 文件时出错:', err);
            reject(err);
        });
    });
}

zipDirectory().catch(err => {
    console.error('创建 zip 文件失败:', err);
    process.exit(1);
});
