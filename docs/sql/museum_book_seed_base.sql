-- Base seed: heads, relics, message templates, demo admin
-- Admin password is plaintext "admin123"; AdminServiceImpl accepts plaintext once then upgrades to MD5.

SET NAMES utf8mb4;
USE museum_book;

TRUNCATE TABLE `head`;
INSERT INTO `head` (`_id`, `HEAD_PIC_ID`, `HEAD_PIC_URL`, `_pid`) VALUES
('1', 'head_1', '/images/avatars/1.png', '1'),
('2', 'head_2', '/images/avatars/2.png', '1'),
('3', 'head_3', '/images/avatars/3.png', '1'),
('4', 'head_4', '/images/avatars/4.png', '1'),
('5', 'head_5', '/images/avatars/5.png', '1'),
('6', 'head_6', '/images/avatars/6.png', '1'),
('7', 'head_7', '/images/avatars/7.png', '1'),
('8', 'head_8', '/images/avatars/8.png', '1'),
('9', 'head_9', '/images/avatars/9.png', '1'),
('10', 'head_10', '/images/avatars/10.png', '1'),
('11', 'head_11', '/images/avatars/11.png', '1'),
('12', 'head_12', '/images/avatars/12.png', '1');

TRUNCATE TABLE `relic`;
INSERT INTO `relic` (`_id`, `RELIC_NAME`, `RELIC_DESC`, `RELIC_IMAGE`) VALUES
('0', '马踏飞燕 (Bronze Galloping Horse)', '东汉时期青铜器。1969年出土于甘肃省武威市雷台汉墓。', '/images/relics/0.png'),
('1', '四羊方尊', '商朝晚期青铜礼器，祭祀用品。', '/images/relics/1.png'),
('2', '长信宫灯', '西汉青铜器。1968年出土于河北省满城县。', '/images/relics/2.png'),
('3', '后母戊鼎', '商周时期青铜器。是现存最重的青铜器。', '/images/relics/3.png'),
('4', '唐三彩', '唐代低温釉陶器的总称。', '/images/relics/4.png');

TRUNCATE TABLE `sys_message_template`;
INSERT INTO `sys_message_template` (`code`, `title_template`, `content_template`, `update_time`) VALUES
('BOOKING_SUCCESS', '预约成功通知', '{0}，时间：{1}。请准时参观。', NOW()),
('BOOKING_CANCEL', '预约取消通知', '您预约的 {0} 已成功取消。', NOW()),
('ACTIVITY_NEW', '新活动通知', '新活动 {0} 已经上线，快来查看吧！', NOW()),
('MUSEUM_NEW', '新展览发布', '博物馆有新的展览：{0}，欢迎预约参观。', NOW());

DELETE FROM `admin` WHERE `ADMIN_NAME` = 'admin';
INSERT INTO `admin` (
  `_id`, `ADMIN_NAME`, `ADMIN_PASSWORD`, `ADMIN_ID`, `ADMIN_ADD_TIME`, `_pid`,
  `ADMIN_NICKNAME`, `ADMIN_INTRO`, `ADMIN_AVATAR`, `ADMIN_INFO_UPDATE_TIME`
) VALUES (
  'admin_demo_001',
  'admin',
  'admin123',
  'admin_demo_001',
  UNIX_TIMESTAMP() * 1000,
  '1',
  '管理员',
  '管理员，负责场馆预约系统管理',
  '/src/assets/avatars/1.jpg',
  UNIX_TIMESTAMP() * 1000
);
