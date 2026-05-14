import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import type { MatchSuggestion } from '@/api/matching.api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { useProfPublic } from '@/hooks/queries/useProfile';
import { initials } from '@/lib/format';

/**
 * One ranked prof card in the discovery grid. Resolves the prof's public
 * profile via `/api/v1/profiles/profs/{id}` so we can show the display name +
 * avatar in addition to the matching engine's score / pricing fields.
 */
export function MatchCard({ match }: { match: MatchSuggestion }) {
  const { t } = useTranslation();
  const profQuery = useProfPublic(match.profId);
  const prof = profQuery.data;

  const scorePct = Math.round(match.score * 100);

  return (
    <Card className="flex h-full flex-col">
      <CardHeader className="flex flex-row items-center gap-3 space-y-0">
        <Avatar>
          <AvatarImage src={prof?.photoUrl ?? undefined} alt="" />
          <AvatarFallback>{initials(prof?.displayName)}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="truncate text-base font-semibold">
            {prof?.displayName ?? '—'}
          </div>
          {prof?.locations && prof.locations.length > 0 && (
            <div className="truncate text-xs text-muted-foreground">
              {prof.locations[0].city}
            </div>
          )}
        </div>
        <Badge variant="info">{scorePct}%</Badge>
      </CardHeader>
      <CardContent className="flex-1 space-y-2 text-sm">
        {match.rationale && (
          <p className="line-clamp-3 text-muted-foreground">{match.rationale}</p>
        )}
        {(match.basePriceMad || match.totalPriceMad) && (
          <div className="text-xs">
            {match.totalPriceMad && (
              <span className="font-medium">
                {match.totalPriceMad} MAD
              </span>
            )}
            {match.basePriceMad && !match.totalPriceMad && (
              <span className="font-medium">
                {match.basePriceMad} MAD
              </span>
            )}
          </div>
        )}
      </CardContent>
      <CardFooter>
        <Button asChild variant="outline" className="w-full">
          <Link to={`/student/profs/${match.profId}`}>{t('discover.view_profile')}</Link>
        </Button>
      </CardFooter>
    </Card>
  );
}
