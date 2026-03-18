import React, { useState, useRef, useEffect } from "react";
import NavBar from "../Navbar/NavBar";
import "./ChatPage.css";
import { Send, Bot, User, Loader } from "lucide-react";

interface Message {
  role: "user" | "assistant";
  content: string;
}

const ChatPage: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMessage: Message = { role: "user", content: input.trim() };
    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setLoading(true);

    try {
      const response = await fetch(`${import.meta.env.VITE_CHAT_API}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: userMessage.content }),
      });

      if (response.ok) {
        const data = await response.json();
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: data.answer },
        ]);
      } else {
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: "Sorry, something went wrong. Please try again." },
        ]);
      }
    } catch (err) {
      console.error(err);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "Unable to reach the server." },
      ]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="chat-layout">
      <NavBar />
      <div className="chat-content">
        <div className="chat-container">
          <div className="chat-header">
            <Bot size={22} />
            <h2>Glucose Assistant</h2>
          </div>

          <div className="chat-messages">
            {messages.length === 0 && (
              <div className="chat-empty">
                <Bot size={40} className="empty-icon" />
                <p>Ask me about your glucose patterns and food impact</p>
                <div className="chat-suggestions">
                  <button onClick={() => setInput("Compare the last 3 times I had biryani")}>
                    Compare last 3 biryani spikes
                  </button>
                  <button onClick={() => setInput("Why did my glucose spike after lunch today?")}>
                    Why did my glucose spike after lunch?
                  </button>
                  <button onClick={() => setInput("What foods cause the highest spikes for me?")}>
                    What foods cause highest spikes?
                  </button>
                </div>
              </div>
            )}

            {messages.map((msg, idx) => (
              <div key={idx} className={`chat-message ${msg.role}`}>
                <div className="message-icon">
                  {msg.role === "user" ? <User size={18} /> : <Bot size={18} />}
                </div>
                <div className="message-bubble">
                  {msg.content}
                </div>
              </div>
            ))}

            {loading && (
              <div className="chat-message assistant">
                <div className="message-icon">
                  <Bot size={18} />
                </div>
                <div className="message-bubble typing">
                  <Loader size={16} className="spin" />
                  <span>Analyzing your data...</span>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          <div className="chat-input-bar">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about your glucose patterns..."
              disabled={loading}
            />
            <button onClick={handleSend} disabled={loading || !input.trim()}>
              <Send size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatPage;