import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Mic, MicOff, Video, VideoOff } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';

/**
 * Pre-call device check. Uses `getUserMedia` directly — does NOT contact
 * LiveKit yet. We stop tracks on unmount to avoid keeping the camera light
 * on when the user navigates away.
 */
export function DeviceCheckPanel() {
  const { t } = useTranslation();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [camOn, setCamOn] = useState(true);
  const [micOn, setMicOn] = useState(true);
  const [micLevel, setMicLevel] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let audioCtx: AudioContext | null = null;
    let analyser: AnalyserNode | null = null;
    let raf = 0;

    (async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true,
        });
        if (cancelled) {
          stream.getTracks().forEach((t2) => t2.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play().catch(() => {
            /* autoplay can be blocked — UI still works */
          });
        }
        // Audio level meter.
        try {
          const AC =
            window.AudioContext ||
            (window as unknown as { webkitAudioContext: typeof AudioContext })
              .webkitAudioContext;
          audioCtx = new AC();
          const source = audioCtx.createMediaStreamSource(stream);
          analyser = audioCtx.createAnalyser();
          analyser.fftSize = 256;
          source.connect(analyser);
          const data = new Uint8Array(analyser.frequencyBinCount);
          const tick = () => {
            if (!analyser) return;
            analyser.getByteFrequencyData(data);
            let sum = 0;
            for (let i = 0; i < data.length; i += 1) sum += data[i];
            const avg = sum / data.length;
            setMicLevel(Math.min(100, Math.round((avg / 255) * 100)));
            raf = requestAnimationFrame(tick);
          };
          tick();
        } catch {
          /* audio meter is best-effort */
        }
      } catch {
        if (!cancelled) {
          setError(t('video.device_check.error'));
        }
      }
    })();

    return () => {
      cancelled = true;
      if (raf) cancelAnimationFrame(raf);
      if (audioCtx) void audioCtx.close();
      const s = streamRef.current;
      if (s) s.getTracks().forEach((tr) => tr.stop());
    };
  }, [t]);

  const toggleCam = () => {
    const s = streamRef.current;
    if (!s) return;
    s.getVideoTracks().forEach((tr) => {
      tr.enabled = !tr.enabled;
    });
    setCamOn((on) => !on);
  };

  const toggleMic = () => {
    const s = streamRef.current;
    if (!s) return;
    s.getAudioTracks().forEach((tr) => {
      tr.enabled = !tr.enabled;
    });
    setMicOn((on) => !on);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">{t('video.device_check.title')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {error ? (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : (
          <video
            ref={videoRef}
            playsInline
            muted
            className="aspect-video w-full rounded-md bg-muted object-cover"
            aria-label={t('video.device_check.preview_alt')}
          />
        )}

        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>{t('video.device_check.mic_level')}</span>
            <span aria-live="polite">{micLevel}%</span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full bg-primary transition-all"
              style={{ width: `${micLevel}%` }}
              aria-hidden="true"
            />
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" size="sm" onClick={toggleCam}>
            {camOn ? <Video className="h-4 w-4" /> : <VideoOff className="h-4 w-4" />}
            <span className="ms-2">
              {camOn ? t('video.controls.camera_off') : t('video.controls.camera_on')}
            </span>
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={toggleMic}>
            {micOn ? <Mic className="h-4 w-4" /> : <MicOff className="h-4 w-4" />}
            <span className="ms-2">
              {micOn ? t('video.controls.mic_off') : t('video.controls.mic_on')}
            </span>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
