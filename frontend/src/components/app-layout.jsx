import { AppSidebar } from "@/components/app-sidebar"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"

export function AppLayout({ children, ...sidebarProps }) {
  return (
    <SidebarProvider>
      <AppSidebar {...sidebarProps} />
      <SidebarInset className="gap-8 bg-muted p-4 md:p-9">
        <SidebarTrigger className="md:hidden" />
        {children}
      </SidebarInset>
    </SidebarProvider>
  )
}
