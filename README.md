🔥 Sample Seed Data
👤 USER (5명)
INSERT INTO USER (Name, Email, Password, Biography) VALUES
('Jinyoung', 'jin@test.com', '1234', 'Backend Developer 지망생입니다.'),
('Minji',    'min@test.com', '1234', '디자인과 코딩을 좋아해요.'),
('Namjin',   'nam@test.com', '1234', 'SQL 쿼리 마스터'),
('GuestUser','guest@test.com','1234','구경 왔습니다.'),
('Professor','prof@test.com','1234','과제 채점용 계정');

📝 POST (5개)
INSERT INTO POST (User_id, Post_context, File_name, Upload_time) VALUES
(1,'드디어 DB 워크샵 프로젝트 끝! 다들 너무 고생했어.',NULL,'2023-11-28 14:00:00'),
(2,'우리집 고양이 보고 가세요. 너무 귀엽죠?','cat.jpg','2023-11-28 15:30:00'),
(3,'새벽 코딩 중... SQL JOIN이 왜 이렇게 안 되지?','code_error.png','2023-11-29 01:00:00'),
(1,'오늘 점심은 학식 돈까스! 가성비 최고.','lunch.png','2023-11-29 12:30:00'),
(4,'안녕하세요. 가입 인사 드립니다! 잘 부탁드려요.',NULL,'2023-11-30 09:00:00');

💬 COMMENT (댓글 예시)
INSERT INTO COMMENT (Post_id, User_id, Comment) VALUES
(1,2,'진짜 고생했다! 발표 준비만 잘 하자.'),
(1,3,'오늘 저녁에 회식 고고?'),
(2,1,'와 진짜 귀엽다... 나만 고양이 없어.'),
(3,2,'오타 난 거 아니야? 스펠링 확인해봐.'),
(5,1,'반갑습니다! 환영해요.');

👍 REACTION (좋아요 / 싫어요)
INSERT INTO REACTION (User_id, Post_id, Type) VALUES
(2,1,'LIKE'),(3,1,'LIKE'),(4,1,'LIKE'),     -- 1번 게시글: 좋아요 3개
(1,2,'LIKE'),(3,2,'LIKE'),                 -- 2번 게시글: 좋아요 2개
(2,3,'LIKE'),(4,3,'DISLIKE'),              -- 3번 게시글: 1 LIKE / 1 DISLIKE
(1,5,'LIKE');                              -- 5번 게시글: LIKE 1개
