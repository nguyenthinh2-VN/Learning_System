import { useEffect, useRef, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useChatbotStore } from '@/store/useChatbotStore';
import { createSessionApi, getMessagesApi, sendMessageApi } from '@/api/chatbotApi';
import { getCourseByIdApi } from '@/api/course';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { MessageCircle, X, Send, Plus, Sparkles, Loader2, Info, History, ArrowLeft, BookOpen, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { Link } from 'react-router-dom';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs) {
  return twMerge(clsx(inputs));
}

function CourseCardCarouselItem({ courseId, setIsOpen }) {
  const [course, setCourse] = useState(null);

  useEffect(() => {
    getCourseByIdApi(courseId).then(res => setCourse(res.data.data)).catch(() => { });
  }, [courseId]);

  return (
    <Link
      to={`/courses/${courseId}`}
      onClick={() => setIsOpen(false)}
      className="flex-shrink-0 w-44 snap-center flex flex-col gap-2 p-3 rounded-xl border bg-background hover:bg-muted/50 transition-colors text-foreground no-underline shadow-sm"
    >
      <div className="h-24 bg-muted/50 rounded-lg flex items-center justify-center overflow-hidden">
        {course?.thumbnailUrl ? (
          <img src={course.thumbnailUrl} alt={course?.title || 'Course'} className="w-full h-full object-cover" />
        ) : (
          <BookOpen className="h-8 w-8 text-muted-foreground/50" />
        )}
      </div>
      <div className="flex flex-col gap-0.5 h-[36px]">
        {course ? (
          <span className="font-semibold text-[13px] line-clamp-2 leading-snug">{course.title}</span>
        ) : (
          <span className="font-semibold text-[13px] line-clamp-1">Khóa học #{courseId}</span>
        )}
      </div>
      <Button variant="secondary" size="sm" className="h-7 mt-auto text-[11px] rounded-full w-full font-medium">Xem khóa học</Button>
    </Link>
  );
}

export default function ChatbotWidget() {
  const { isPublicAuthenticated } = useAuth();
  const {
    isOpen,
    setIsOpen,
    sessionId,
    setSessionId,
    messages,
    setMessages,
    addMessage,
    isLoading,
    setIsLoading,
    resetSession,
    sessionHistory,
    deleteSession
  } = useChatbotStore();

  const [inputStr, setInputStr] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const messagesEndRef = useRef(null);

  // Cuộn xuống tin nhắn cuối cùng
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen]);

  // Load lịch sử tin nhắn khi mở nếu có sessionId
  useEffect(() => {
    const loadMessages = async () => {
      if (isOpen && isPublicAuthenticated && sessionId) {
        try {
          const res = await getMessagesApi(sessionId);
          // Backend trả về mảng trực tiếp trong res.data.data
          setMessages(res.data.data || []);
        } catch (error) {
          console.error("Failed to load messages", error);
          if (error.response?.status === 404) {
            resetSession();
          }
        } finally {
          setIsLoading(false);
        }
      }
    };
    loadMessages();
  }, [isOpen, isPublicAuthenticated, sessionId, setMessages, resetSession, setIsLoading]);

  const toggleOpen = () => {
    if (!isOpen && !isPublicAuthenticated) {
      toast.error('Vui lòng đăng nhập để sử dụng tính năng tư vấn AI');
    }
    setIsOpen(!isOpen);
    setShowHistory(false);
  };

  const handleSelectSession = (id) => {
    setSessionId(id);
    setShowHistory(false);
  };

  const handleNewSession = async () => {
    if (!isPublicAuthenticated) return;
    try {
      setIsLoading(true);
      const res = await createSessionApi();
      setSessionId(res.data.data.id);
      setMessages([]);
    } catch (error) {
      toast.error('Lỗi khi tạo phiên tư vấn mới');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendMessage = async (e) => {
    e?.preventDefault();
    if (!inputStr.trim() || isLoading || !isPublicAuthenticated) return;

    let currentSessionId = sessionId;

    try {
      // Nếu chưa có session, tạo session mới ngay trước khi gửi
      if (!currentSessionId) {
        setIsLoading(true);
        const res = await createSessionApi();
        currentSessionId = res.data.data.id;
        setSessionId(currentSessionId);
      }

      // Thêm message của user vào UI ngay lập tức
      const userMsg = { role: 'USER', content: inputStr };
      addMessage(userMsg);
      const msgToSend = inputStr;
      setInputStr('');
      setIsLoading(true);

      const res = await sendMessageApi(currentSessionId, msgToSend);
      // Backend trả về SendMessageOutput { replyMessage, recommendedCourseIds }
      addMessage({
        role: 'ASSISTANT',
        content: res.data.data.replyMessage,
        recommendedCourses: res.data.data.recommendedCourseIds
      });

    } catch (error) {
      toast.error('Gửi tin nhắn thất bại');
      console.error(error);
      // Remove the optimistic message if it failed or keep it with error state (simplified here)
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) {
    return (
      <button
        onClick={toggleOpen}
        className="fixed flex items-center justify-center bottom-6 right-6 h-14 w-14 rounded-full shadow-lg shadow-indigo-500/30 hover:shadow-indigo-500/50 hover:-translate-y-1 transition-all duration-300 z-50 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 text-white"
      >
        <Sparkles className="h-6 w-6" />
      </button>
    );
  }

  return (
    <div className="fixed inset-0 sm:inset-auto sm:bottom-24 sm:right-6 sm:w-[400px] sm:h-[600px] sm:max-h-[calc(100vh-120px)] bg-background sm:border rounded-none sm:rounded-2xl shadow-2xl flex flex-col z-50 overflow-hidden animate-in slide-in-from-bottom-5">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b bg-primary/5">
        <div className="flex items-center gap-3">
          {showHistory ? (
            <Button variant="ghost" size="icon" onClick={() => setShowHistory(false)} className="h-8 w-8 -ml-2 text-muted-foreground">
              <ArrowLeft className="h-5 w-5" />
            </Button>
          ) : (
            <div className="flex items-center justify-center h-10 w-10 rounded-full bg-primary/10 text-primary">
              <Sparkles className="h-5 w-5" />
            </div>
          )}
          <div>
            <h3 className="font-semibold text-sm">{showHistory ? 'Lịch sử tư vấn' : 'Learning Advisor AI'}</h3>
            {!showHistory && (
              <p className="text-xs text-muted-foreground flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-green-500 inline-block"></span>
                Trực tuyến
              </p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-1">
          {isPublicAuthenticated && !showHistory && (
            <>
              <Button variant="ghost" size="icon" onClick={() => setShowHistory(true)} className="h-8 w-8 text-muted-foreground hover:text-foreground" title="Lịch sử">
                <History className="h-4 w-4" />
              </Button>
              <Button variant="ghost" size="icon" onClick={handleNewSession} className="h-8 w-8 text-muted-foreground hover:text-foreground" title="Phiên mới">
                <Plus className="h-4 w-4" />
              </Button>
            </>
          )}
          <Button variant="ghost" size="icon" onClick={() => setIsOpen(false)} className="h-8 w-8 text-muted-foreground hover:text-foreground">
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-4 bg-muted/20">
        {showHistory ? (
          <div className="flex flex-col gap-2">
            {sessionHistory.length === 0 ? (
              <div className="text-center text-sm text-muted-foreground mt-10">Chưa có phiên tư vấn nào.</div>
            ) : (
              sessionHistory.map((s) => (
                <div
                  key={s.id}
                  className={cn(
                    "flex flex-row items-center justify-between p-2 pl-3 rounded-xl border bg-background hover:bg-muted/50 transition-colors group relative",
                    s.id === sessionId && "border-primary ring-1 ring-primary/20"
                  )}
                >
                  <button
                    onClick={() => handleSelectSession(s.id)}
                    className="flex-1 flex flex-col text-left pr-8"
                  >
                    <span className="font-medium text-sm">{s.title}</span>
                    <span className="text-xs text-muted-foreground mt-1">
                      {new Date(s.updatedAt).toLocaleString('vi-VN')}
                    </span>
                  </button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 text-muted-foreground hover:text-destructive hover:bg-destructive/10 opacity-0 group-hover:opacity-100 transition-opacity shrink-0 absolute right-2"
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteSession(s.id);
                    }}
                    title="Xóa phiên"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              ))
            )}
          </div>
        ) : !isPublicAuthenticated ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center gap-4 px-6">
            <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-2">
              <Info className="h-8 w-8" />
            </div>
            <h3 className="font-semibold text-lg">Tính năng dành cho học viên</h3>
            <p className="text-sm text-muted-foreground">
              Vui lòng đăng nhập để trò chuyện với AI Learning Advisor và nhận lộ trình học tập cá nhân hóa.
            </p>
            <Button asChild className="mt-2 w-full rounded-full">
              <Link to="/login" onClick={() => setIsOpen(false)}>Đăng nhập ngay</Link>
            </Button>
          </div>
        ) : messages.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center px-6">
            <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-4">
              <Sparkles className="h-8 w-8" />
            </div>
            <p className="text-sm text-muted-foreground">
              Xin chào! Tôi là AI cố vấn học tập của bạn. Tôi có thể giúp bạn định hướng nghề nghiệp, chọn khóa học phù hợp, hoặc giải đáp các thắc mắc về lộ trình.
            </p>
          </div>
        ) : (
          messages.map((msg, idx) => (
            <div
              key={idx}
              className={cn(
                "max-w-[85%] rounded-2xl px-4 py-2.5 text-sm",
                msg.role === 'USER'
                  ? "bg-primary text-primary-foreground rounded-br-sm self-end"
                  : "bg-background border rounded-tl-sm self-start shadow-sm"
              )}
            >
              <div className="whitespace-pre-wrap leading-relaxed">{msg.content}</div>

              {/* Hiển thị Course Card nếu có recommendations */}
              {(() => {
                let courseIds = msg.recommendedCourses || msg.recommendedCourseIds;
                if (typeof courseIds === 'string') {
                  try {
                    courseIds = JSON.parse(courseIds);
                  } catch (e) {
                    courseIds = [];
                  }
                }
                if (!Array.isArray(courseIds) || courseIds.length === 0) return null;

                return (
                  <div className="mt-3 flex flex-col gap-2">
                    <div className="text-xs font-semibold text-primary mb-1">Các khóa học được gợi ý:</div>
                    <div className="flex overflow-x-auto snap-x hide-scrollbar gap-3 pb-2 -mx-2 px-2">
                      {courseIds.map(id => (
                        <CourseCardCarouselItem key={id} courseId={id} setIsOpen={setIsOpen} />
                      ))}
                    </div>
                  </div>
                );
              })()}
            </div>
          ))
        )}
        {isLoading && (
          <div className="bg-background border rounded-2xl rounded-tl-sm self-start px-4 py-3 shadow-sm flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin text-primary" />
            <span className="text-xs text-muted-foreground">AI đang suy nghĩ...</span>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Footer / Input (Only if authenticated) */}
      {isPublicAuthenticated && !showHistory && (
        <div className="p-3 bg-background border-t">
          <form onSubmit={handleSendMessage} className="flex items-end gap-2 bg-muted/50 rounded-2xl p-1 border focus-within:ring-1 focus-within:ring-primary focus-within:border-primary transition-all">
            <Input
              value={inputStr}
              onChange={(e) => setInputStr(e.target.value)}
              placeholder="Hỏi về khóa học, kỹ năng..."
              className="border-0 bg-transparent shadow-none focus-visible:ring-0 focus-visible:ring-offset-0 min-h-[44px] px-3"
              disabled={isLoading}
            />
            <Button
              type="submit"
              size="icon"
              disabled={!inputStr.trim() || isLoading}
              className="h-10 w-10 rounded-full shrink-0 mb-[2px] mr-[2px]"
            >
              <Send className="h-4 w-4" />
            </Button>
          </form>
          <div className="text-center mt-2">
            <span className="text-[10px] text-muted-foreground opacity-70">AI có thể mắc lỗi. Vui lòng kiểm tra lại thông tin.</span>
          </div>
        </div>
      )}
    </div>
  );
}
