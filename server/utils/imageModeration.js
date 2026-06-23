const fs = require('fs');
const path = require('path');

// 直接读取 .env 文件（不依赖 dotenv/process.env，避免 pm2 环境变量问题）
function loadEnv() {
  try {
    const envPath = path.join(__dirname, '..', '.env');
    const envContent = fs.readFileSync(envPath, 'utf8');
    const env = {};
    envContent.split('\n').forEach(line => {
      const match = line.match(/^([^#=]+)=(.*)$/);
      if (match) {
        env[match[1].trim()] = match[2].trim();
      }
    });
    return env;
  } catch (e) {
    console.warn('[图片审核] 读取 .env 文件失败:', e.message);
    return {};
  }
}

const env = loadEnv();
const accessKeyId = env.ALIYUN_ACCESS_KEY_ID;
const accessKeySecret = env.ALIYUN_ACCESS_KEY_SECRET;

const Green = require('@alicloud/green20220302');
const { Config } = require('@alicloud/openapi-client');
const { RuntimeOptions } = require('@alicloud/tea-util');

// 初始化客户端（延迟初始化）
let client = null;

function getClient() {
  if (!client) {
    if (!accessKeyId || !accessKeySecret) {
      console.warn('[图片审核] ALIYUN_ACCESS_KEY_ID 或 ALIYUN_ACCESS_KEY_SECRET 未配置，图片审核功能已禁用');
      return null;
    }
    const config = new Config({
      accessKeyId,
      accessKeySecret,
      endpoint: 'green-cip.cn-shanghai.aliyuncs.com'
    });
    client = new Green.default(config);
  }
  return client;
}

/**
 * 审核图片（同步接口）
 * @param {string} imageUrl - 图片 URL（支持相对路径 /uploads/xxx）
 * @returns {Promise<{passed: boolean, riskLevel: string, labels: string[]}>}
 */
async function moderateImage(imageUrl) {
  const greenClient = getClient();
  if (!greenClient) {
    return { passed: true, riskLevel: 'none', labels: [] };
  }

  // 将相对路径拼接为完整 URL（阿里云需要公网可访问的 URL）
  let fullUrl = imageUrl;
  if (imageUrl.startsWith('/uploads/')) {
    fullUrl = `http://111.229.166.216:3002${imageUrl}`;
  }

  try {
    const request = new Green.ImageModerationRequest({
      service: 'baselineCheck',
      serviceParameters: JSON.stringify({
        imageUrl: fullUrl,
        dataId: 'goodx-' + Date.now()
      })
    });

    const runtime = new RuntimeOptions({});
    const response = await greenClient.imageModerationWithOptions(request, runtime);

    const body = response.body;
    if (body.code !== 200) {
      console.error('[图片审核] 接口返回错误:', body.msg);
      return { passed: true, riskLevel: 'error', labels: [] };
    }

    const data = body.data || {};
    const result = data.result || [];
    
    // 解析风险等级
    let riskLevel = 'none';
    const labels = [];
    
    for (const item of result) {
      if (item.confidence && item.confidence > 60) {
        riskLevel = 'high';
      } else if (item.confidence && item.confidence > 30) {
        riskLevel = 'medium';
      }
      labels.push(item.label);
    }

    // high = 高风险，直接拦截
    // medium = 中风险，可以拦截或标记审核
    const passed = riskLevel !== 'high' && riskLevel !== 'medium';

    return { passed, riskLevel, labels };
  } catch (error) {
    console.error('[图片审核] 调用失败:', error.message);
    // 审核服务异常时，默认放行（避免影响正常发帖）
    return { passed: true, riskLevel: 'error', labels: [] };
  }
}

/**
 * 批量审核多张图片
 * @param {string[]} imageUrls - 图片 URL 数组
 * @returns {Promise<{passed: boolean, results: Array}>}
 */
async function moderateImages(imageUrls) {
  if (!imageUrls || imageUrls.length === 0) {
    return { passed: true, results: [] };
  }

  const results = [];
  for (const url of imageUrls) {
    const result = await moderateImage(url);
    results.push({ url, ...result });
    if (!result.passed) {
      // 有一张不通过就整体拦截
      return { passed: false, results };
    }
  }

  return { passed: true, results };
}

module.exports = {
  moderateImage,
  moderateImages
};
