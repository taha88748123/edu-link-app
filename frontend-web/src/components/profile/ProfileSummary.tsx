import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import type { ProfPublicView } from '@/api/profile.api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { initials } from '@/lib/format';

/**
 * Header card on the prof's public profile page. Renders avatar, display name,
 * bio, subjects, modalities and locations. No contact info exposed — masked
 * by design at the backend layer.
 */
export function ProfileSummary({ prof }: { prof: ProfPublicView }) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardContent className="space-y-6 p-6">
        <div className="flex flex-col items-start gap-4 sm:flex-row">
          <Avatar className="h-20 w-20">
            <AvatarImage src={prof.photoUrl ?? undefined} alt="" />
            <AvatarFallback className="text-lg">{initials(prof.displayName)}</AvatarFallback>
          </Avatar>
          <div className="flex-1 space-y-1">
            <h1 className="text-2xl font-semibold">{prof.displayName ?? '—'}</h1>
            {prof.yearsExperience != null && (
              <p className="text-sm text-muted-foreground">
                {t('profile.experience_years', { count: prof.yearsExperience })}
              </p>
            )}
            <p className="text-sm">
              {t('profile.rating_label', {
                stars: Number(prof.avgStars).toFixed(1),
                count: prof.reviewCount,
              })}
            </p>
          </div>
        </div>

        <Separator />

        <Section title={t('profile.bio')}>
          {prof.bio ? (
            <p className="whitespace-pre-line text-sm">{prof.bio}</p>
          ) : (
            <p className="text-sm text-muted-foreground">{t('profile.no_bio')}</p>
          )}
        </Section>

        <Section title={t('profile.subjects')}>
          <div className="flex flex-wrap gap-1">
            {prof.subjects.map((s) => (
              <Badge key={`${s.code}-${s.level}`} variant="secondary">
                {s.code} · {t(`level.${s.level}`)}
              </Badge>
            ))}
            {prof.subjects.length === 0 && (
              <span className="text-sm text-muted-foreground">{t('common.none')}</span>
            )}
          </div>
        </Section>

        <Section title={t('profile.modalities')}>
          <div className="flex flex-wrap gap-1">
            {prof.modalities.map((m) => (
              <Badge key={m} variant="outline">
                {t(`modality.${m}`)}
              </Badge>
            ))}
            {prof.modalities.length === 0 && (
              <span className="text-sm text-muted-foreground">{t('common.none')}</span>
            )}
          </div>
        </Section>

        {prof.locations.length > 0 && (
          <Section title={t('profile.locations')}>
            <div className="flex flex-wrap gap-1">
              {prof.locations.map((l) => (
                <Badge key={`${l.city}-${l.region ?? ''}`} variant="outline">
                  {l.city}
                  {l.region ? ` · ${l.region}` : ''}
                </Badge>
              ))}
            </div>
          </Section>
        )}
      </CardContent>
    </Card>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-2">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        {title}
      </h2>
      <div>{children}</div>
    </section>
  );
}
