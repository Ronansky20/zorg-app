import {
  RiArrowDownSLine,
  RiExpandUpDownLine,
  RiGalleryLine,
  RiLogoutBoxRLine,
  RiSearchLine,
  RiTerminalLine,
} from "@remixicon/react"
import { professionLabel } from "@/lib/patients"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible"
import { DropdownMenu, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { FieldError } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  useSidebar,
} from "@/components/ui/sidebar"

const NAV_ITEMS = [
  { id: "dashboard", label: "Dashboard" },
  { id: "search", label: "Search patients" },
  { id: "edit", label: "Edit Patients", action: "EDIT_PATIENT" },
  { id: "add", label: "Add Patients", action: "ADD_PATIENT" },
]

function initials(name) {
  return name.split(/\s+/).filter((part) => /^[A-Z]/.test(part) && !part.endsWith(".")).map((part) => part[0]).slice(0, 2).join("")
}

export function AppSidebar({ me, active, query, onQueryChange, onNavigate, onLogout, logoutError }) {
  const items = NAV_ITEMS.filter((item) => !item.action || me.actions.includes(item.action))
  const { setOpenMobile } = useSidebar()

  function navigate(id) {
    setOpenMobile(false)
    onNavigate(id)
  }

  return (
    <Sidebar className="md:border md:border-sidebar-border md:rounded-lg md:overflow-hidden **:data-[slot=sidebar-inner]:bg-card">
      <SidebarHeader>
        <div className="flex items-center gap-2 p-2">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
            <RiGalleryLine className="size-4" />
          </div>
          <span className="flex-1 truncate text-sm font-medium">Zorg App</span>
          <RiExpandUpDownLine className="size-4" />
        </div>
        <InputGroup className="mx-2 w-auto bg-background">
          <InputGroupInput aria-label="Search patients" placeholder="Search" value={query}
            onChange={(event) => onQueryChange(event.target.value)} />
          <InputGroupAddon>
            <RiSearchLine />
          </InputGroupAddon>
        </InputGroup>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarMenu className="gap-1">
            <SidebarMenuItem>
              <Collapsible defaultExpanded className="group/collapsible">
                <SidebarMenuButton slot="trigger">
                  <RiTerminalLine />
                  <span className="flex-1">Main Menu</span>
                  <RiArrowDownSLine className="-rotate-90 transition-transform group-data-expanded/collapsible:rotate-0" />
                </SidebarMenuButton>
                <CollapsibleContent>
                  <SidebarMenuSub>
                    {items.map((item) => (
                      <SidebarMenuSubItem key={item.id}>
                        <SidebarMenuSubButton className="w-full" isActive={active === item.id}
                          onPress={() => navigate(item.id)}>
                          <span>{item.label}</span>
                        </SidebarMenuSubButton>
                      </SidebarMenuSubItem>
                    ))}
                  </SidebarMenuSub>
                </CollapsibleContent>
              </Collapsible>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <DropdownMenuTrigger>
              <SidebarMenuButton size="lg" className="h-auto">
                <Avatar>
                  <AvatarFallback className="bg-sidebar-primary text-xs text-sidebar-primary-foreground">
                    {initials(me.user.name)}
                  </AvatarFallback>
                </Avatar>
                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate text-sm font-medium">{me.user.name}</span>
                  <span className="truncate text-xs">{professionLabel(me.user.profession)}</span>
                </div>
                <RiExpandUpDownLine />
              </SidebarMenuButton>
              <DropdownMenu placement="top start">
                <DropdownMenuItem textValue="Log out" onAction={onLogout}>
                  <RiLogoutBoxRLine />
                  Log out
                </DropdownMenuItem>
              </DropdownMenu>
            </DropdownMenuTrigger>
          </SidebarMenuItem>
        </SidebarMenu>
        {logoutError && <FieldError className="px-2">{logoutError}</FieldError>}
      </SidebarFooter>
    </Sidebar>
  )
}
