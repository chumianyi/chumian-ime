// 初眠输入法服务端 - UDP 24512端口
const dgram = require('dgram');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const PORT = 24512;
const HOST = '0.0.0.0';

// 数据存储
const DATA_DIR = path.join(__dirname, 'data');
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

const usersFile = path.join(DATA_DIR, 'users.json');
const skinsFile = path.join(DATA_DIR, 'skins.json');
const statsFile = path.join(DATA_DIR, 'stats.json');
const signinsFile = path.join(DATA_DIR, 'signins.json');

let users = JSON.parse(fs.existsSync(usersFile) ? fs.readFileSync(usersFile) : '{}');
let stats = JSON.parse(fs.existsSync(statsFile) ? fs.readFileSync(statsFile) : '{}');
let signins = JSON.parse(fs.existsSync(signinsFile) ? fs.readFileSync(signinsFile) : '{}');

// 皮肤数据
const skins = [
  { id: 'aurora', name: '极光紫', colors: [0xFF6C5CE7, 0xFFA29BFE], price: 0 },
  { id: 'sakura', name: '樱花粉', colors: [0xFFFF9A9E, 0xFFFAD0C4], price: 0 },
  { id: 'ocean', name: '海洋蓝', colors: [0xFF4FACFE, 0xFF00F2FE], price: 0 },
  { id: 'forest', name: '森林绿', colors: [0xFF43E97B, 0xFF38F9D7], price: 0 },
  { id: 'sunset', name: '日落橙', colors: [0xFFFF6B6B, 0xFFFECA57], price: 1 },
  { id: 'starry', name: '星空黑', colors: [0xFF2D3436, 0xFF636E72], price: 1 },
  { id: 'mint', name: '薄荷青', colors: [0xFF00B894, 0xFF55EFC4], price: 1 },
  { id: 'rosegold', name: '玫瑰金', colors: [0xFFE17055, 0xFFFAB1A0], price: 1 },
  { id: 'lavender', name: '薰衣草', colors: [0xFFA29BFE, 0xFFD6A2E8], price: 1 },
  { id: 'coral', name: '珊瑚红', colors: [0xFFFF7675, 0xFFFF9FF3], price: 1 },
  { id: 'emerald', name: '翡翠绿', colors: [0xFF00CEC9, 0xFF81ECEC], price: 1 },
  { id: 'royal', name: '皇家蓝', colors: [0xFF0984E3, 0xFF74B9FF], price: 1 },
];

// 词库数据
const dictionaries = [
  { id: 'basic', name: '基础词库', desc: '常用汉字和词语', size: '2.3MB', words: 50000 },
  { id: 'internet', name: '网络流行词', desc: '最新网络用语和梗', size: '1.2MB', words: 20000 },
  { id: 'tech', name: '科技词库', desc: 'IT和科技专业词汇', size: '3.1MB', words: 80000 },
  { id: 'medical', name: '医学词库', desc: '医学专业术语', size: '4.5MB', words: 120000 },
  { id: 'legal', name: '法律词库', desc: '法律专业词汇', size: '2.8MB', words: 60000 },
  { id: 'finance', name: '金融词库', desc: '金融和经济词汇', size: '3.5MB', words: 90000 },
  { id: 'game', name: '游戏词库', desc: '游戏相关词汇', size: '1.8MB', words: 40000 },
  { id: 'anime', name: '动漫词库', desc: '动漫和二次元词汇', size: '2.1MB', words: 55000 },
  { id: 'food', name: '美食词库', desc: '美食和烹饪词汇', size: '1.5MB', words: 35000 },
  { id: 'travel', name: '旅游词库', desc: '旅游和出行词汇', size: '1.9MB', words: 45000 },
  { id: 'sports', name: '体育词库', desc: '体育和运动词汇', size: '2.2MB', words: 50000 },
  { id: 'music', name: '音乐词库', desc: '音乐和娱乐词汇', size: '1.7MB', words: 40000 },
  { id: 'art', name: '艺术词库', desc: '艺术和设计词汇', size: '2.0MB', words: 48000 },
  { id: 'history', name: '历史词库', desc: '历史和文化词汇', size: '3.2MB', words: 75000 },
  { id: 'geography', name: '地理词库', desc: '地理和地名词汇', size: '2.5MB', words: 60000 },
  { id: 'philosophy', name: '哲学词库', desc: '哲学和思想词汇', size: '1.8MB', words: 42000 },
  { id: 'psychology', name: '心理词库', desc: '心理学词汇', size: '2.3MB', words: 55000 },
  { id: 'education', name: '教育词库', desc: '教育和学习词汇', size: '2.6MB', words: 65000 },
  { id: 'business', name: '商业词库', desc: '商业和管理词汇', size: '3.0MB', words: 70000 },
  { id: 'agriculture', name: '农业词库', desc: '农业和种植词汇', size: '1.6MB', words: 38000 },
];

function saveData() {
  fs.writeFileSync(usersFile, JSON.stringify(users));
  fs.writeFileSync(statsFile, JSON.stringify(stats));
  fs.writeFileSync(signinsFile, JSON.stringify(signins));
}

function hashPassword(password) {
  return crypto.createHash('sha256').update(password + 'chumian_ime_salt').digest('hex');
}

function generateToken(username) {
  return crypto.createHash('sha256').update(username + Date.now() + Math.random()).digest('hex');
}

function verifyToken(token) {
  for (const [username, user] of Object.entries(users)) {
    if (user.token === token) return username;
  }
  return null;
}

const server = dgram.createSocket('udp4');

server.on('error', (err) => {
  console.error(`服务器错误: ${err.stack}`);
  server.close();
});

server.on('message', (msg, rinfo) => {
  let request;
  try {
    request = JSON.parse(msg.toString());
  } catch (e) {
    sendResponse(rinfo, { error: '无效请求' });
    return;
  }

  const { action, data } = request;
  let response = {};

  try {
    switch (action) {
      case 'register': {
        const { username, password, invite_code } = data;
        if (!username || !password || !invite_code) {
          response = { error: '缺少参数' };
          break;
        }
        if (users[username]) {
          response = { error: '用户名已存在' };
          break;
        }
        const token = generateToken(username);
        users[username] = {
          password: hashPassword(password),
          token,
          meow_coins: 0,
          owned_skins: ['aurora', 'sakura', 'ocean', 'forest'],
          created_at: Date.now(),
        };
        stats[username] = { total_chars: 0, total_inputs: 0, accuracy: 100 };
        saveData();
        response = { success: true, token, meow_coins: 0 };
        break;
      }

      case 'login': {
        const { username, password } = data;
        const user = users[username];
        if (!user || user.password !== hashPassword(password)) {
          response = { error: '用户名或密码错误' };
          break;
        }
        if (user.banned && user.banned_until > Date.now()) {
          response = { error: '账号已封禁' };
          break;
        }
        const token = generateToken(username);
        user.token = token;
        saveData();
        response = { success: true, token, meow_coins: user.meow_coins };
        break;
      }

      case 'signin': {
        const username = verifyToken(data.token);
        if (!username) { response = { error: '未登录' }; break; }
        const today = new Date().toDateString();
        if (signins[username] === today) {
          response = { error: '今日已签到' };
          break;
        }
        signins[username] = today;
        users[username].meow_coins += 1;
        saveData();
        response = { success: true, meow_coins: users[username].meow_coins };
        break;
      }

      case 'get_skins': {
        const username = verifyToken(data.token);
        const owned = username ? users[username].owned_skins : [];
        response = {
          skins: skins.map(s => ({
            ...s,
            owned: owned.includes(s.id),
          })),
        };
        break;
      }

      case 'buy_skin': {
        const username = verifyToken(data.token);
        if (!username) { response = { error: '未登录' }; break; }
        const skin = skins.find(s => s.id === data.skin_id);
        if (!skin) { response = { error: '皮肤不存在' }; break; }
        if (users[username].owned_skins.includes(skin.id)) {
          response = { error: '已拥有' };
          break;
        }
        if (users[username].meow_coins < skin.price) {
          response = { error: '喵喵币不足' };
          break;
        }
        users[username].meow_coins -= skin.price;
        users[username].owned_skins.push(skin.id);
        saveData();
        response = { success: true, meow_coins: users[username].meow_coins };
        break;
      }

      case 'get_dictionaries': {
        response = { dictionaries };
        break;
      }

      case 'update_stats': {
        const username = verifyToken(data.token);
        if (!username) { response = { error: '未登录' }; break; }
        if (!stats[username]) stats[username] = { total_chars: 0, total_inputs: 0, accuracy: 100 };
        stats[username].total_chars += data.char_count || 0;
        stats[username].total_inputs += 1;
        saveData();
        response = { success: true };
        break;
      }

      case 'get_stats': {
        const username = verifyToken(data.token);
        if (!username) { response = { error: '未登录' }; break; }
        response = { stats: stats[username] || {} };
        break;
      }

      case 'check_update': {
        response = { has_update: false, latest_version: '1.0.0' };
        break;
      }

      default:
        response = { error: '未知操作' };
    }
  } catch (e) {
    response = { error: '服务器错误' };
  }

  sendResponse(rinfo, response);
});

function sendResponse(rinfo, data) {
  const msg = Buffer.from(JSON.stringify(data));
  server.send(msg, rinfo.port, rinfo.address, (err) => {
    if (err) console.error('发送失败:', err);
  });
}

server.on('listening', () => {
  const address = server.address();
  console.log(`初眠输入法服务端运行在 ${address.address}:${address.port}`);
});

server.bind(PORT, HOST);
