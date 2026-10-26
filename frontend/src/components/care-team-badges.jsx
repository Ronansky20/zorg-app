import { useConfig } from "@/hooks/use-config"
import { professionLabel } from "@/lib/patients"
import { Badge } from "@/components/ui/badge"

export function CareTeamBadges({ careTeam, me }) {
  const config = useConfig()
  return (
    <div className="flex flex-wrap gap-2">
      {careTeam.map((member) => (
        <Badge key={member.id} variant="outline">
          {member.name ?? member.id}{member.id === me.user.id && " (you)"}
          {member.profession && ` · ${professionLabel(config, member.profession)}`}
        </Badge>
      ))}
    </div>
  )
}
