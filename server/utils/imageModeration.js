const { default: Green2022Client } = require('@alicloud/green20220302');
const { Config } = require('@alicloud/openapi-client');

// 从环境变量读取阿里云配置
const accessKeyId = process.env.ALIYUN_ACCESS_KEY_ID;
const accessKeySecret = process.env.ALIYUN_ACCESS_KEY_SECRET;

// 初始化客户端（延迟初始化，避免启动时 AK/SK 未配置就报错）
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
    client = new Green2022Client(config);
  }
  return client;
}

/**
 * 审核图片
 * @param {string} imageUrl - 图片 URL
 * @returns {Promise<{passed: boolean, riskLevel: string, labels: string[]}>}
 */
async function moderateImage(imageUrl) {
  const greenClient = getClient();
  if (!greenClient) {
    // 未配置 AK/SK，默认放行
    return { passed: true, riskLevel: 'none', labels: [] };
  }

  try {
    const response = await greenClient.imageModeration({
      service: 'image_moderation', // 图片审核增强版
      serviceParameters: JSON.stringify({ url: imageUrl })
    });

    const result = JSON.parse(response.body.data);
    const riskLevel = result.riskLevel || 'none'; // none / low / medium / high
    const labels = result.result || [];

    // high = 高风险，直接拦截
    // medium = 中风险，可以拦截或标记审核
    // low / none = 放行
    const passed = riskLevel !== 'high' && riskLevel !== 'medium';

    return {
      passed,
      riskLevel,
      labels: labels.map(l => l.label)
    };
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
