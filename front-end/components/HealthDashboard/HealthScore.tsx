import React, { useState, useEffect } from "react";
import "./HealthScore.css";

interface ScoreBreakdown {
    score: number;
    weight: string;
    label: string;
}

interface HealthScoreData {
    score: number;
    glucose: ScoreBreakdown;
    heartRate: ScoreBreakdown;
    activity: ScoreBreakdown;
    recovery: ScoreBreakdown;
}

interface HealthScoreProps {
    selectedDay: string | null;
}

const HealthScore: React.FC<HealthScoreProps> = ({ selectedDay }) => {
    const [data, setData] = useState<HealthScoreData | null>(null);
    const [loading, setLoading] = useState(false);
    const [animatedScore, setAnimatedScore] = useState(0);

    useEffect(() => {
        if (selectedDay) fetchScore(selectedDay);
    }, [selectedDay]);

    useEffect(() => {
        if (data) {
            let current = 0;
            const target = data.score;
            const step = target / 40;
            const timer = setInterval(() => {
                current += step;
                if (current >= target) { current = target; clearInterval(timer); }
                setAnimatedScore(Math.round(current));
            }, 20);
            return () => clearInterval(timer);
        }
    }, [data]);

    const fetchScore = async (day: string) => {
        setLoading(true);
        try {
            const res = await fetch(`/api/health-events/score/${day}`);
            const json = await res.json();
            setData(json);
        } catch (e) {
            console.error("Failed to fetch health score", e);
        }
        setLoading(false);
    };

    const getScoreColor = (score: number) => {
        if (score >= 80) return "#4ade80";
        if (score >= 50) return "#fbbf24";
        if (score >= 20) return "#fb923c";
        return "#ef4444";
    };

    const getMood = (score: number): string => {
        if (score >= 80) return "ecstatic";
        if (score >= 50) return "happy";
        if (score >= 20) return "tired";
        return "sleeping";
    };

    const getLabel = (score: number): string => {
        if (score >= 80) return "Beast Mode";
        if (score >= 50) return "Solid Day";
        if (score >= 20) return "Rest Day";
        return "Recovering";
    };

    const score = data?.score ?? 0;
    const color = getScoreColor(score);
    const mood = getMood(score);
    const circumference = 2 * Math.PI * 50;
    const offset = circumference - (animatedScore / 100) * circumference;

    const renderCharacter = () => (
        <svg viewBox="0 0 200 280" className={`hs-svg-char hs-svg-char--${mood}`} xmlns="http://www.w3.org/2000/svg">
            {/* Barbell */}
            <g className="hs-svg-barbell">
                {/* Bar */}
                <rect x="20" y="58" width="160" height="5" rx="2.5" fill="#9ca3af"/>
                {/* Left weights */}
                <rect x="10" y="42" width="18" height="36" rx="4" fill="#3b82f6"/>
                <rect x="2" y="48" width="14" height="24" rx="3" fill="#2563eb"/>
                {/* Right weights */}
                <rect x="172" y="42" width="18" height="36" rx="4" fill="#3b82f6"/>
                <rect x="184" y="48" width="14" height="24" rx="3" fill="#2563eb"/>
            </g>

            {/* Left arm */}
            <g className="hs-svg-arm-left">
                {/* Upper arm */}
                <ellipse cx="62" cy="130" rx="16" ry="26" fill="#d4a574"/>
                {/* Bicep bulge */}
                <ellipse cx="56" cy="122" rx="12" ry="10" fill="#c4956a"/>
                {/* Forearm */}
                <ellipse cx="48" cy="85" rx="10" ry="22" fill="#d4a574"/>
                {/* Hand */}
                <circle cx="46" cy="64" r="9" fill="#d4a574"/>
                {/* Fingers gripping bar */}
                <rect x="38" y="56" width="16" height="8" rx="3" fill="#c4956a"/>
            </g>

            {/* Right arm */}
            <g className="hs-svg-arm-right">
                <ellipse cx="138" cy="130" rx="16" ry="26" fill="#d4a574"/>
                <ellipse cx="144" cy="122" rx="12" ry="10" fill="#c4956a"/>
                <ellipse cx="152" cy="85" rx="10" ry="22" fill="#d4a574"/>
                <circle cx="154" cy="64" r="9" fill="#d4a574"/>
                <rect x="146" y="56" width="16" height="8" rx="3" fill="#c4956a"/>
            </g>

            {/* Body / Tank top */}
            <path d="M72 110 Q68 108 68 115 L66 170 Q66 178 80 180 L120 180 Q134 178 134 170 L132 115 Q132 108 128 110 Z" fill="#f59e0b"/>
            {/* Tank top straps */}
            <path d="M82 98 L76 110 L84 112 Z" fill="#f59e0b"/>
            <path d="M118 98 L124 110 L116 112 Z" fill="#f59e0b"/>
            {/* Belt */}
            <rect x="68" y="168" width="64" height="12" rx="3" fill="#92400e"/>
            <rect x="94" y="168" width="12" height="12" rx="2" fill="#fbbf24"/>

            {/* Neck */}
            <rect x="88" y="92" width="24" height="14" rx="5" fill="#d4a574"/>

            {/* Head */}
            <ellipse cx="100" cy="72" rx="28" ry="30" fill="#d4a574"/>

            {/* Hair / top */}
            <path d="M72 62 Q72 38 100 36 Q128 38 128 62 Q125 50 100 48 Q75 50 72 62 Z" fill="#1e293b"/>
            {/* Hair curls on top */}
            <circle cx="88" cy="42" r="6" fill="#1e293b"/>
            <circle cx="100" cy="39" r="7" fill="#1e293b"/>
            <circle cx="112" cy="42" r="6" fill="#1e293b"/>

            {/* Beard */}
            <path d="M78 78 Q78 104 100 108 Q122 104 122 78 Q118 92 100 96 Q82 92 78 78 Z" fill="#1e293b"/>
            {/* Mustache */}
            <path d="M86 80 Q93 86 100 80 Q107 86 114 80 Q107 82 100 78 Q93 82 86 80 Z" fill="#292524"/>

            {/* Eyes */}
            <g className="hs-svg-eyes">
                <ellipse cx="88" cy="66" rx="5" ry="5" fill="white"/>
                <circle className="hs-svg-pupil" cx="88" cy="66" r="2.5" fill="#1e293b"/>
                <ellipse cx="112" cy="66" rx="5" ry="5" fill="white"/>
                <circle className="hs-svg-pupil" cx="112" cy="66" r="2.5" fill="#1e293b"/>
            </g>

            {/* Eyebrows */}
            <g className="hs-svg-brows">
                <path d="M80 58 Q88 54 96 58" stroke="#1e293b" strokeWidth="3" fill="none" strokeLinecap="round"/>
                <path d="M104 58 Q112 54 120 58" stroke="#1e293b" strokeWidth="3" fill="none" strokeLinecap="round"/>
            </g>

            {/* Mouth (changes by mood) */}
            {mood === "ecstatic" && <path d="M88 86 Q100 78 112 86" stroke="#fff" strokeWidth="2" fill="none" strokeLinecap="round"/>}
            {mood === "happy" && <path d="M90 84 Q100 80 110 84" stroke="#fff" strokeWidth="2" fill="none" strokeLinecap="round"/>}

            {/* Shorts */}
            <path d="M68 180 L66 218 L88 218 L92 190 L108 190 L112 218 L134 218 L132 180 Z" fill="#1e3a5f"/>

            {/* Legs */}
            <rect x="70" y="218" width="22" height="36" rx="8" fill="#d4a574"/>
            <rect x="108" y="218" width="22" height="36" rx="8" fill="#d4a574"/>

            {/* Shoes */}
            <path d="M66 252 L92 252 Q96 252 96 256 L96 264 Q96 268 92 268 L62 268 Q58 268 58 264 L58 260 Q58 254 66 252 Z" fill="#1e293b"/>
            <path d="M108 252 L134 252 Q138 252 142 260 L142 264 Q142 268 138 268 L108 268 Q104 268 104 264 L104 256 Q104 252 108 252 Z" fill="#1e293b"/>

            {/* Sweat for tired */}
            {mood === "tired" && (
                <g className="hs-svg-sweat">
                    <ellipse cx="130" cy="58" rx="4" ry="6" fill="#7dd3fc" opacity="0.8"/>
                    <ellipse cx="136" cy="68" rx="3" ry="5" fill="#7dd3fc" opacity="0.6"/>
                </g>
            )}

            {/* Zzz for sleeping */}
            {mood === "sleeping" && (
                <g className="hs-svg-zzz">
                    <text x="135" y="40" fill="#94a3b8" fontSize="18" fontWeight="bold" fontFamily="DM Sans">Z</text>
                    <text x="148" y="30" fill="#94a3b8" fontSize="14" fontWeight="bold" fontFamily="DM Sans" opacity="0.7">z</text>
                    <text x="158" y="22" fill="#94a3b8" fontSize="10" fontWeight="bold" fontFamily="DM Sans" opacity="0.4">z</text>
                </g>
            )}
        </svg>
    );

    return (
        <div className="hs-container">
            {loading ? (
                <div className="hs-loading"><div className="hs-loading-dot"></div><span>Computing health score...</span></div>
            ) : data ? (
                <>
                    <div className="hs-left">
                        <div className={`hs-scene hs-scene--${mood}`}>
                            <div className="hs-sky">
                                {(mood === "ecstatic" || mood === "happy") && <div className="hs-sun"></div>}
                                {mood === "tired" && (<><div className="hs-cloud hs-cloud--grey hs-cloud--1"></div><div className="hs-cloud hs-cloud--grey hs-cloud--2"></div></>)}
                                {mood === "sleeping" && (<><div className="hs-cloud hs-cloud--dark hs-cloud--1"></div><div className="hs-cloud hs-cloud--dark hs-cloud--2"></div><div className="hs-rain"></div></>)}
                                {mood === "ecstatic" && (<><div className="hs-sparkle hs-sparkle--1">✦</div><div className="hs-sparkle hs-sparkle--2">✦</div><div className="hs-sparkle hs-sparkle--3">✦</div></>)}
                            </div>
                            <div className="hs-ground"></div>
                            {renderCharacter()}
                        </div>

                        <div className="hs-ring-wrap">
                            <svg className="hs-ring" viewBox="0 0 110 110">
                                <circle cx="55" cy="55" r="50" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="7" />
                                <circle cx="55" cy="55" r="50" fill="none" stroke={color} strokeWidth="7" strokeLinecap="round" strokeDasharray={circumference} strokeDashoffset={offset} transform="rotate(-90 55 55)" style={{ transition: "stroke-dashoffset 0.8s ease" }} />
                            </svg>
                            <div className="hs-ring-text">
                                <div className="hs-ring-score" style={{ color }}>{animatedScore}</div>
                                <div className="hs-ring-label">{getLabel(score)}</div>
                            </div>
                        </div>
                    </div>

                    <div className="hs-right">
                        <div className="hs-title-row">
                            <span className="hs-title-icon">💪</span>
                            <span className="hs-title">Health Score</span>
                        </div>
                        <div className="hs-breakdown">
                            {[data.glucose, data.heartRate, data.activity, data.recovery].map((item, i) => (
                                <div className="hs-bar-row" key={i}>
                                    <div className="hs-bar-label">
                                        <span className="hs-bar-name">{item.label}</span>
                                        <span className="hs-bar-weight">{item.weight}</span>
                                    </div>
                                    <div className="hs-bar-track">
                                        <div className="hs-bar-fill" style={{ width: `${item.score}%`, backgroundColor: getScoreColor(item.score), transition: "width 0.6s ease" }}></div>
                                    </div>
                                    <span className="hs-bar-value" style={{ color: getScoreColor(item.score) }}>{item.score}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </>
            ) : null}
        </div>
    );
};

export default HealthScore;